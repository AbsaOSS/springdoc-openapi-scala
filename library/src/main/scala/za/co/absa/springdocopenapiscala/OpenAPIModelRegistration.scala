/*
 * Copyright 2023 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.springdocopenapiscala

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.{Discriminator, Schema}

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import java.util.UUID
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import OpenAPIModelRegistration._

class OpenAPIModelRegistration(
  components: Components,
  extraTypesHandler: ExtraTypesHandling.ExtraTypesHandler = ExtraTypesHandling.noExtraHandling,
  config: RegistrationConfig = RegistrationConfig()
) {

  /**
   *  Registers given top-level type.
   *  Top-level means one that is directly either response from some endpoint or is in request body.
   *  All child case classes of a case class are registered automatically by this method,
   *  they don't need to be registered separately.
   *
   *  For example for such model:
   *  {{{
   *    case class A(someField: String)
   *    case class B(someField: String)
   *    case class Request(a: A)
   *    case class Response(b: B)
   *  }}}
   *  and endpoint:
   *  {{{
   *    @PostMapping(value = Array("/some-endpoint"))
   *    @ResponseStatus(value = HttpStatus.OK)
   *    def someEndpoint(@RequestBody request: Request): CompletableFuture[Response]
   *  }}}
   *  one has to register only `Request` and `Response`
   *  (for example in the same controller where the endpoint is defined):
   *  {{{
   *    openAPIModelRegistration.register[Request]
   *    openAPIModelRegistration.register[Response]
   *  }}}
   *  and `A` and `B` will be registered automatically.
   *
   *  To support custom types not supported by the library,
   *  construct [[OpenAPIModelRegistration]] with [[OpenAPIModelRegistration.ExtraTypesHandling.ExtraTypesHandler]].
   */
  def register[T: TypeTag](): Unit = {
    val tpe = typeOf[T]
    handleType(tpe)
  }

  private case class OpenAPISimpleType(tpe: String, format: Option[String] = None)

  @tailrec
  private def handleType(tpe: Type): Schema[_] = {
    if (extraTypesHandler.isDefinedAt(tpe)) handleExtraTypes(tpe)
    else
      tpe.dealias match {
        case t if tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass => handleCaseType(t)
        case t if t <:< typeOf[Map[_, _]]                                      => handleMap(t)
        case t if t <:< typeOf[Option[_]]                                      => handleType(t.typeArgs.head)
        case t if t <:< typeOf[Seq[_]] || t <:< typeOf[Array[_]]               => handleSeqLike(t)
        case t if t <:< typeOf[Set[_]]                                         => handleSet(t)
        case t if t <:< typeOf[Enumeration#Value]                              => handleEnum(t)
        case t if t.typeSymbol.isClass && t.typeSymbol.asClass.isSealed        => handleSealedType(t)
        case t                                                                 => handleSimpleType(t)
      }
  }

  private def handleExtraTypes(tpe: Type): Schema[_] = {
    val (childTypesToBeResolved, handleFn) = extraTypesHandler(tpe)
    val resolvedChildTypes: Map[Type, Schema[_]] = childTypesToBeResolved.map(t => (t, handleType(t))).toMap
    val context = RegistrationContext(components)
    handleFn(resolvedChildTypes, context)
  }

  private def handleCaseType(tpe: Type): Schema[_] = {
    val name = tpe.typeSymbol.name.toString.trim
    val schema = new Schema
    schema.setType("object")
    val fields = tpe.decls.collect {
      case field: TermSymbol if field.isVal => field
    }
    fields.foreach { f =>
      val fieldName = f.name.toString.trim
      val childSchema = handleType(f.typeSignature)
      schema.addProperty(fieldName, childSchema)
      val isOption = f.typeSignature <:< typeOf[Option[_]]
      if (!isOption) schema.addRequiredItem(fieldName)
    }

    registerAsReference(name, schema)
  }

  private def handleMap(tpe: Type): Schema[_] = {
    val schema = new Schema
    schema.setType("object")
    schema
  }

  private def handleSeqLike(tpe: Type): Schema[_] = {
    val schema = new Schema
    val innerSchema = handleType(tpe.typeArgs.head)
    schema.setType("array")
    schema.setItems(innerSchema)
    schema
  }

  private def handleSet(tpe: Type): Schema[_] = {
    // Set doesn't exist in JSON
    handleSeqLike(tpe)
  }

  private def handleEnum(tpe: Type): Schema[_] = {
    val TypeRef(parentObjectType, _, _) = tpe
    val enumValues = parentObjectType.members.filter(isSymbolEnumerationValue)
    val enumValuesAsStrings = enumValues.map(_.name.toString.trim)

    val schema = new Schema[String]
    schema.setType("string")
    schema.setEnum(enumValuesAsStrings.toList.asJava)
    schema
  }

  private def isSymbolEnumerationValue(s: Symbol): Boolean =
    s.isTerm && s.asTerm.isVal && s.typeSignature <:< typeOf[Enumeration#Value]

  private def handleSealedType(tpe: Type): Schema[_] = {

    def addDiscriminatorPropertyToChildren(
      currentSchema: Schema[_],
      discriminatorPropertyName: String,
      addOnlyToDirectChildren: Boolean,
      discriminatorValue: Option[String] = None,
      seen: Set[String] = Set.empty
    ): Unit = {
      val children = currentSchema.getOneOf.asScala
      children.foreach { s =>
        val ref = s.get$ref
        val name = extractSchemaNameFromRef(ref)
        val actualSchema = components.getSchemas.get(name)
        if (actualSchema.getType == "object") {
          val constEnumSchema = createConstEnumSchema(discriminatorValue.getOrElse(name))
          actualSchema.addProperty(discriminatorPropertyName, constEnumSchema)
          actualSchema.addRequiredItem(discriminatorPropertyName)
        } else if (
          !addOnlyToDirectChildren &&
          !seen.contains(name) &&
          Option(actualSchema.getOneOf).map(!_.isEmpty).getOrElse(false) // is schema representing another sum ADT root
        ) {
          addDiscriminatorPropertyToChildren(
            actualSchema,
            discriminatorPropertyName,
            addOnlyToDirectChildren,
            Some(name),
            seen + name
          )
        }
      }
    }

    val classSymbol = tpe.typeSymbol.asClass
    val name = tpe.typeSymbol.name.toString.trim

    // in case of recursive ADT, it might already have been processed, thus we should skip
    val wasAlreadyProcessed = Option(components.getSchemas).map(_.containsKey(name)).getOrElse(false)

    if (wasAlreadyProcessed) {
      (new Schema).$ref(s"#/components/schemas/$name")
    } else {
      val children = classSymbol.knownDirectSubclasses
      // we can assume that all sum ADT direct children are registered as reference, as these can be:
      // - case classes = registered as reference
      // - case objects = registered as reference
      // - sealed trait/abstract class = registered as reference
      val childrenRefs = children.map(s => (new Schema).$ref(s.name.toString.trim)).toSeq
      val schema = new Schema
      schema.setOneOf(childrenRefs.asJava)
      val schemaRef = registerAsReference(name, schema)
      children.map(_.asType.toType).foreach(handleType)

      config.sumADTsShape match {
        case RegistrationConfig.SumADTsShape.WithDiscriminator(discriminatorPropertyNameFn, addOnlyToDirectChildren) =>
          val discriminatorPropertyName = discriminatorPropertyNameFn(name)
          schema.setDiscriminator {
            val discriminator = new Discriminator
            discriminator.setPropertyName(discriminatorPropertyName)
            discriminator
          }
          addDiscriminatorPropertyToChildren(
            schema,
            discriminatorPropertyName,
            addOnlyToDirectChildren
          )

        case _ => ()
      }

      schemaRef
    }
  }

  private def handleSimpleType(tpe: Type): Schema[_] = {
    val schema = new Schema
    val OpenAPISimpleType(terminalTpe, format) = getOpenAPISimpleType(tpe)
    schema.setType(terminalTpe)
    format.foreach(f => schema.setFormat(f))
    schema
  }

  private def getOpenAPISimpleType(tpe: Type): OpenAPISimpleType = tpe.dealias match {
    case t if t =:= typeOf[Byte]          => OpenAPISimpleType("integer", Some("int32"))
    case t if t =:= typeOf[Short]         => OpenAPISimpleType("integer", Some("int32"))
    case t if t =:= typeOf[Int]           => OpenAPISimpleType("integer", Some("int32"))
    case t if t =:= typeOf[Long]          => OpenAPISimpleType("integer", Some("int64"))
    case t if t =:= typeOf[Float]         => OpenAPISimpleType("number", Some("float"))
    case t if t =:= typeOf[Double]        => OpenAPISimpleType("number", Some("double"))
    case t if t =:= typeOf[Char]          => OpenAPISimpleType("string")
    case t if t =:= typeOf[String]        => OpenAPISimpleType("string")
    case t if t =:= typeOf[UUID]          => OpenAPISimpleType("string", Some("uuid"))
    case t if t =:= typeOf[Boolean]       => OpenAPISimpleType("boolean")
    case t if t =:= typeOf[Unit]          => OpenAPISimpleType("null")
    case t if t =:= typeOf[ZonedDateTime] => OpenAPISimpleType("string", Some("date-time"))
    case t if t =:= typeOf[Instant]       => OpenAPISimpleType("string", Some("date-time"))
    case t if t =:= typeOf[LocalDateTime] => OpenAPISimpleType("string", Some("date-time"))
    case t if t =:= typeOf[LocalDate]     => OpenAPISimpleType("string", Some("date"))
  }

  private def registerAsReference(name: String, schema: Schema[_]): Schema[_] = {
    components.addSchemas(name, schema)
    val schemaReference = new Schema
    schemaReference.set$ref(s"#/components/schemas/$name")
    schemaReference
  }

  private def createConstEnumSchema(const: String): Schema[_] = {
    val constEnumSchema = new Schema[String]
    constEnumSchema.setType("string")
    constEnumSchema.setEnum(Seq(const).asJava)
    constEnumSchema
  }

  private def extractSchemaNameFromRef(ref: String): String = {
    ref.substring(ref.lastIndexOf("/") + 1)
  }

}

object OpenAPIModelRegistration {

  /**
   *  Configuration of the registration class.
   *
   *  @param sumADTsShape how sum ADTs should be registered (with or without discriminator)
   */
  case class RegistrationConfig(
    sumADTsShape: RegistrationConfig.SumADTsShape = RegistrationConfig.SumADTsShape.WithoutDiscriminator
  )

  object RegistrationConfig {

    sealed abstract class SumADTsShape

    object SumADTsShape {
      case object WithoutDiscriminator extends SumADTsShape
      case class WithDiscriminator(
        discriminatorPropertyNameFn: WithDiscriminator.DiscriminatorPropertyNameFn =
          WithDiscriminator.defaultDiscriminatorPropertyNameFn,
        addDiscriminatorPropertyOnlyToDirectChildren: Boolean = true
      ) extends SumADTsShape

      object WithDiscriminator {

        /** Function from sealed type name to discriminator property name. */
        type DiscriminatorPropertyNameFn = String => String

        val defaultDiscriminatorPropertyNameFn: DiscriminatorPropertyNameFn = sealedTypeName =>
          sealedTypeName.head.toLower + sealedTypeName.tail + "Type"
      }
    }

  }

  /**
   *  Context of model registration.
   *  Currently contains only `Components` that can be mutated if needed
   *  (for example to add schema object to be used as schema reference by other types).
   */
  case class RegistrationContext(components: Components)

  object ExtraTypesHandling {

    /**
     *  ExtraTypesHandler is a partial function which can be used to:
     *  - handle custom types that are not supported by the library.
     *  - overwrite handling of types that are supported by the library (for example to include some custom format)
     *
     *  It takes [[Type]] as an input, and should produce a pair of:
     *  - [[ChildTypesToBeResolved]] which is a set of types which the ExtraTypesHandler
     *    needs to be resolved by the library before it can perform its handling
     *  - [[HandleFn]] which performs the handling;
     *    it is a function which takes [[ResolvedChildTypes]]
     *    (map of [[ChildTypesToBeResolved]] to [[Schema]] resolved by the library)
     *    and [[RegistrationContext]], and should perform all the handling needed and at the end return [[Schema]]
     */
    type ExtraTypesHandler = PartialFunction[
      Type,
      (ChildTypesToBeResolved, HandleFn)
    ]

    type ChildTypesToBeResolved = Set[Type]
    type ResolvedChildTypes = Map[Type, Schema[_]]
    type HandleFn = (ResolvedChildTypes, RegistrationContext) => Schema[_]

    /**
     *  [[ExtraTypesHandler]] which doesn't add/overwrite support for any type.
     */
    val noExtraHandling: ExtraTypesHandler = PartialFunction.empty

    /**
     *  Creates simple [[ExtraTypesHandler]] which doesn't need [[RegistrationContext]]
     *  nor doesn't have child types to be resolved by the library.
     *
     *  Example:
     *  {{{
     *    ExtraTypesHandling.simpleMapping {
     *      case t if t =:= typeOf[JsonNode] =>
     *        val schema = new Schema
     *        schema.setType("string")
     *        schema.setFormat("json")
     *        schema
     *    }
     *  }}}
     */
    def simpleMapping(getSchemaOfTypes: PartialFunction[Type, Schema[_]]): ExtraTypesHandler =
      getSchemaOfTypes.andThen { schema =>
        (Set.empty, (_, _) => schema)
      }

  }

}

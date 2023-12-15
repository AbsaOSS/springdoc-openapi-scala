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
import io.swagger.v3.oas.models.media.Schema

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import java.util.UUID
import scala.annotation.tailrec
import scala.reflect.runtime.universe._

class OpenAPIModelRegistration(components: Components) {

  /**
   *  Registers given top-level case class.
   *  Top-level means one that is directly either response from some endpoint or is in request body.
   *  All child case classes are registered automatically by this method, they don't need to be registered separately.
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
   */
  def register[T: TypeTag](): Unit = {
    val tpe = typeOf[T]
    handleType(tpe)
  }

  private case class OpenAPISimpleType(tpe: String, format: Option[String] = None)

  @tailrec
  private def handleType(tpe: Type): Schema[_] = tpe.dealias match {
    case t if tpe.typeSymbol.isClass && tpe.typeSymbol.asClass.isCaseClass => handleCaseClass(t)
    case t if t <:< typeOf[Map[_, _]]                                      => handleMap(t)
    case t if t <:< typeOf[Option[_]]                                      => handleType(t.typeArgs.head)
    case t if t <:< typeOf[Seq[_]] || t <:< typeOf[Array[_]]               => handleSeqLike(t)
    case t if t <:< typeOf[Set[_]]                                         => handleSet(t)
    case t                                                                 => handleSimpleType(t)
  }

  private def handleCaseClass(tpe: Type): Schema[_] = {
    val name = tpe.typeSymbol.name.toString.trim
    val schema = new Schema
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
    components.addSchemas(name, schema)
    val schemaReference = new Schema
    schemaReference.set$ref(s"#/components/schemas/$name")
    schemaReference
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

}

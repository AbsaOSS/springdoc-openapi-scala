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
import org.scalatest
import org.scalatest.flatspec.AnyFlatSpec
import za.co.absa.springdocopenapiscala.OpenAPIModelRegistration.ExtraTypesHandling

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import java.util.UUID
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

class OpenAPIModelRegistrationSpec extends AnyFlatSpec {

  private case class OnlySimpleTypes(
    a: String,
    b: Int,
    c: Short,
    d: Long,
    e: Double,
    f: Float,
    g: Byte,
    h: Char,
    i: Boolean,
    j: UUID,
    k: Unit,
    l: ZonedDateTime,
    o: Instant,
    p: LocalDateTime,
    r: LocalDate
  )

  private case class SimpleTypesMaybeInOption(
    a: String,
    b: Option[String],
    c: Option[Int],
    d: ZonedDateTime,
    e: Option[ZonedDateTime]
  )

  private case class ChildChildCaseClass(a: String)

  private case class ChildCaseClass(a: String, b: Int, child: ChildChildCaseClass)

  private case class ParentCaseClass(child: ChildCaseClass, other: String)

  private case class Arrays(
    a: Seq[String],
    b: Seq[Int],
    c: Array[String],
    d: Array[Int],
    e: Set[String],
    f: Set[Int],
    g: List[String],
    h: List[Int]
  )

  private case class Maps(
    a: Map[String, Int],
    b: Map[Int, Arrays]
  )

  private object Things extends Enumeration {
    type Thing = Value

    val Pizza, TV, Radio = Value
  }

  private object DifferentThing extends Enumeration {
    type DifferentThing = Value

    val Cleaner = Value
    val Floor = Value

    val unrelated = "something that is not related to enum"

    def unrelatedDef: Int = 123
  }

  private object ThingsWithRenaming extends Enumeration {
    type ThingWithRenaming = Value

    val Pizza = Value("PIZZA")
    val TV = Value("TV")
    val Radio = Value("RADIO")
  }

  private case class SimpleEnums(
    a: Things.Thing,
    b: DifferentThing.DifferentThing
  )

  private case class EnumsWithRenaming(
    a: ThingsWithRenaming.ThingWithRenaming
  )

  private case class CustomClassComplexChild(a: Option[Int])

  private class CustomClass(val complexChild: CustomClassComplexChild) {
    // these won't be included
    val meaningOfLife: Int = 42
    val alphabetHead: String = "abc"
  }

  private class CustomClassReducingToSimpleType(json: String) {
    def complexLogic: String = json.trim
  }

  private case class ForCustomHandling(
    customClass: CustomClass,
    a: String,
    b: Int,
    customClassReducingToSimpleType: CustomClassReducingToSimpleType
  )

  behavior of "register"

  it should "add schema of a case class that contain fields with simple types to injected components" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[OnlySimpleTypes]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.b", "integer", Some("int32"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.c", "integer", Some("int32"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.d", "integer", Some("int64"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.e", "number", Some("double"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.f", "number", Some("float"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.g", "integer", Some("int32"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.h", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.i", "boolean")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.j", "string", Some("uuid"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.k", "null")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.l", "string", Some("date-time"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.o", "string", Some("date-time"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.p", "string", Some("date-time"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.r", "string", Some("date"))
  }

  it should "mark all non-Option fields of case class as required" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SimpleTypesMaybeInOption]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.b", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.c", "integer", Some("int32"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.d", "string", Some("date-time"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.e", "string", Some("date-time"))

    val actualRequired = actualSchemas.get("SimpleTypesMaybeInOption").getRequired.asScala
    val expectedRequired = Seq("a", "d")

    assert(actualRequired === expectedRequired)
  }

  it should "automatically register case classes used in case class being registered and reference them by name" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[ParentCaseClass]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildChildCaseClass.a", "string")

    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildCaseClass.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildCaseClass.b", "integer", Some("int32"))
    assertRefIsAsExpected(actualSchemas, "ChildCaseClass.child", "#/components/schemas/ChildChildCaseClass")

    assertRefIsAsExpected(actualSchemas, "ParentCaseClass.child", "#/components/schemas/ChildCaseClass")
    assertTypeAndFormatAreAsExpected(actualSchemas, "ParentCaseClass.other", "string")
  }

  it should "make Seq, Set, and Array an OpenAPI array type" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[Arrays]()

    val actualSchemas = components.getSchemas

    assertIsArrayOfExpectedType(actualSchemas, "Arrays.a", "string")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.b", "integer")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.c", "string")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.d", "integer")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.e", "string")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.f", "integer")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.g", "string")
    assertIsArrayOfExpectedType(actualSchemas, "Arrays.h", "integer")
  }

  it should "make Map an OpenAPI object type" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[Maps]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "Maps.a", "object")
    assertTypeAndFormatAreAsExpected(actualSchemas, "Maps.b", "object")
  }

  it should "make simple Enumeration (without renaming) an OpenAPI enum" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SimpleEnums]()

    val actualSchemas = components.getSchemas

    assertEnumIsStringAndHasFollowingOptions(actualSchemas, "SimpleEnums.a", Set("Pizza", "TV", "Radio"))
    assertEnumIsStringAndHasFollowingOptions(actualSchemas, "SimpleEnums.b", Set("Cleaner", "Floor"))
  }

  // this test ignored as current implementation doesn't pass it, to be enabled in #23
  it should "make complex Enumeration (with renaming) an OpenAPI enum" ignore {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[EnumsWithRenaming]()

    val actualSchemas = components.getSchemas

    assertEnumIsStringAndHasFollowingOptions(actualSchemas, "EnumsWithRenaming.a", Set("PIZZA", "TV", "RADIO"))
  }

  it should "use provided ExtraTypesHandler to handle custom types" in {
    import ExtraTypesHandling._

    val components = new Components
    val extraTypesHandler: ExtraTypesHandler = (tpe: Type) =>
      tpe match {
        case t if t =:= typeOf[CustomClass] =>
          val childTypesToBeResolvedByTheLibrary = Set(typeOf[CustomClassComplexChild])
          val handleFn: HandleFn = (resolvedChildTypesSchemas, context) => {
            val name = "CustomClass"
            val customClassComplexChildResolvedSchema = resolvedChildTypesSchemas(typeOf[CustomClassComplexChild])
            val schema = new Schema
            schema.addProperty("complexChild", customClassComplexChildResolvedSchema)
            context.components.addSchemas(name, schema)
            val schemaReference = new Schema
            schemaReference.set$ref(s"#/components/schemas/$name")
            schemaReference
          }
          (childTypesToBeResolvedByTheLibrary, handleFn)

        case t if t =:= typeOf[CustomClassReducingToSimpleType] =>
          val handleFn: HandleFn = (_, _) => {
            val schema = new Schema
            schema.setType("string")
            schema.setFormat("json")
            schema
          }
          (Set.empty, handleFn)
      }
    val openAPIModelRegistration = new OpenAPIModelRegistration(components, extraTypesHandler)

    openAPIModelRegistration.register[ForCustomHandling]()

    val actualSchemas = components.getSchemas

    assertRefIsAsExpected(
      actualSchemas,
      "ForCustomHandling.customClass",
      "#/components/schemas/CustomClass"
    )
    assertTypeAndFormatAreAsExpected(
      actualSchemas,
      "ForCustomHandling.a",
      "string",
      None
    )
    assertTypeAndFormatAreAsExpected(
      actualSchemas,
      "ForCustomHandling.b",
      "integer",
      Some("int32")
    )
    assertTypeAndFormatAreAsExpected(
      actualSchemas,
      "ForCustomHandling.customClassReducingToSimpleType",
      "string",
      Some("json")
    )
    assertRefIsAsExpected(
      actualSchemas,
      "CustomClass.complexChild",
      "#/components/schemas/CustomClassComplexChild"
    )
    assertPredicateForPath(
      actualSchemas,
      "CustomClass",
      schema => schema.getProperties.size == 1
    )
  }

  it should
    "use provided ExtraTypesHandler to overwrite common types that would normally be handled by the library" in {
      val components = new Components
      val extraTypesHandler = ExtraTypesHandling.simpleMapping {
        case t if t =:= typeOf[String] =>
          val schema = new Schema
          schema.setType("string")
          schema.setFormat("my-custom-format")
          schema
      }
      val openAPIModelRegistration = new OpenAPIModelRegistration(components, extraTypesHandler)

      openAPIModelRegistration.register[SimpleTypesMaybeInOption]()

      val actualSchemas = components.getSchemas

      assertTypeAndFormatAreAsExpected(
        actualSchemas,
        "SimpleTypesMaybeInOption.a",
        "string",
        Some("my-custom-format")
      )
      assertTypeAndFormatAreAsExpected(
        actualSchemas,
        "SimpleTypesMaybeInOption.b",
        "string",
        Some("my-custom-format")
      )

      // these are to make sure that types not handled by `extraTypesHandler` still work as expected
      assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.c", "integer", Some("int32"))
      assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.d", "string", Some("date-time"))
      assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption.e", "string", Some("date-time"))
    }

  private def assertTypeAndFormatAreAsExpected(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    expectedType: String,
    expectedFormat: Option[String] = None
  ): scalatest.Assertion = assertPredicateForPath(
    actualSchemas,
    fieldPath,
    schema => schema.getType === expectedType && Option(schema.getFormat) === expectedFormat
  )

  private def assertRefIsAsExpected(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    expectedRef: String
  ): scalatest.Assertion = assertPredicateForPath(
    actualSchemas,
    fieldPath,
    schema => schema.get$ref === expectedRef
  )

  private def assertIsArrayOfExpectedType(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    expectedType: String
  ): scalatest.Assertion = assertPredicateForPath(
    actualSchemas,
    fieldPath,
    schema => schema.getType === "array" && schema.getItems.getType === expectedType
  )

  private def assertEnumIsStringAndHasFollowingOptions(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    expectedEnumOptions: Set[String]
  ): scalatest.Assertion = assertPredicateForPath(
    actualSchemas,
    fieldPath,
    schema => schema.getType === "string" && schema.getEnum.asScala.toSet == expectedEnumOptions
  )

  private def assertPredicateForPath(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    predicate: Schema[_] => Boolean
  ): scalatest.Assertion = {
    val fieldPathSplit = fieldPath.split("\\.")
    assert(actualSchemas.containsKey(fieldPathSplit.head))
    val lastSchema = fieldPathSplit.dropRight(1).foldLeft(actualSchemas) { case (currentSchemas, nextField) =>
      assert(currentSchemas.containsKey(nextField))
      val schema = currentSchemas.get(nextField)
      val properties = schema.getProperties
      assert(Option(properties).nonEmpty)
      properties
    }
    assert(lastSchema.containsKey(fieldPathSplit.last))
    val actualSchema = lastSchema.get(fieldPathSplit.last)
    assert(predicate(actualSchema))
  }

}

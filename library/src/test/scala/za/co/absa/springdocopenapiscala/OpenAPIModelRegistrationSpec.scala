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

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import java.util.UUID
import scala.collection.JavaConverters._

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

  object Things extends Enumeration {
    type Thing = Value

    val Pizza, TV, Radio = Value
  }

  object DifferentThing extends Enumeration {
    type DifferentThing = Value

    val Cleaner = Value
    val Floor = Value

    val unrelated = "something that is not related to enum"

    def unrelatedDef: Int = 123
  }

  object ThingsWithRenaming extends Enumeration {
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

  sealed private trait SealedTrait

  private case class SealedTraitVariant1(a: String) extends SealedTrait

  private case class SealedTraitVariant2(a: Int) extends SealedTrait

  sealed abstract class SealedAbstractClass

  private case object SealedAbstractClassCaseObject extends SealedAbstractClass
  private case class SealedAbstractClassVariant(a: String) extends SealedAbstractClass

  private case class SumTypeClass(a: SealedTrait)

  private case class SumTypeClassWithSealedAbstractClass(a: SealedAbstractClass)

  sealed private trait EmptySealedTrait

  private case class EmptySealedTraitClass(a: EmptySealedTrait)

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

  it should "make sealed type an OpenAPI schema with the oneOf attribute" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SumTypeClass]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedTraitVariant1.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedTraitVariant2.a", "integer",
      Some("int32"))

    assertPredicateForPath(
      actualSchemas,
      "SumTypeClass.a",
      schema => {
        val actualOneOf = schema.getOneOf.asScala
        val expectedOneOf = Seq(
          new Schema().$ref("#/components/schemas/SealedTraitVariant1"),
          new Schema().$ref("#/components/schemas/SealedTraitVariant2")
        )
        actualOneOf === expectedOneOf
      }
    )
  }

  it should "make sealed abstract class with case object an OpenAPI schema with the oneOf attribute" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SumTypeClassWithSealedAbstractClass]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas,"SealedAbstractClassVariant.a", "string")
    assert(actualSchemas.containsKey("SealedAbstractClassCaseObject"))
    assert(Option(actualSchemas.get("SealedAbstractClassCaseObject").getProperties).isEmpty)

    assertPredicateForPath(
      actualSchemas,
      "SumTypeClassWithSealedAbstractClass.a",
      schema => {
        val actualOneOf = schema.getOneOf.asScala
        val expectedOneOf = Seq(
          new Schema().$ref("#/components/schemas/SealedAbstractClassCaseObject"),
          new Schema().$ref("#/components/schemas/SealedAbstractClassVariant")
        )
        actualOneOf === expectedOneOf
      }
    )
  }

  it should "not fail for empty sealed trait" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[EmptySealedTraitClass]()

    val actualSchemas = components.getSchemas

    assertPredicateForPath(
      actualSchemas,
      "EmptySealedTraitClass.a",
      schema => {
        val actualOneOf = schema.getOneOf.asScala
        val expectedOneOf = Seq()
        actualOneOf === expectedOneOf
      }
    )
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

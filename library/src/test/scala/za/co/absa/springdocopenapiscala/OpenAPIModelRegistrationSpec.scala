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

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZonedDateTime}
import java.util.UUID
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._
import za.co.absa.springdocopenapiscala.OpenAPIModelRegistration.ExtraTypesHandling

import java.sql.Timestamp

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
    r: LocalDate,
    s: LocalTime,
    t: BigDecimal,
    w: BigInt,
    z: Timestamp
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
    b: Map[String, Arrays]
  )

  private case class WrongMaps(
    a: Map[Int, Int],
    b: Map[Boolean, Arrays]
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

  private sealed trait NestedSealedTrait

  private case class NestedSealedTraitBreaker(breaker: NestedSealedTrait.NestedSealedTraitVariant1)

  private object NestedSealedTrait {
    case class NestedSealedTraitVariant1(a: String, b: Int, c: NestedSealedTrait) extends NestedSealedTrait
    case object NestedSealedTraitVariant2 extends NestedSealedTrait

    sealed abstract class NestedSealedTraitVariant3 extends NestedSealedTrait

    object NestedSealedTraitVariant3 {
      case class NestedSealedTraitVariant3Subvariant1(a: Float, b: NestedSealedTrait) extends NestedSealedTraitVariant3
      case object NestedSealedTraitVariant3Subvariant2 extends NestedSealedTraitVariant3
    }

  }

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

    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes", "object")
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
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.s", "string", Some("time"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.t", "number")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.w", "integer")
    assertTypeAndFormatAreAsExpected(actualSchemas, "OnlySimpleTypes.z", "string", Some("date-time"))
  }

  it should "mark all non-Option fields of case class as required" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SimpleTypesMaybeInOption]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "SimpleTypesMaybeInOption", "object")
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

    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildChildCaseClass", "object")
    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildChildCaseClass.a", "string")

    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildCaseClass", "object")
    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildCaseClass.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "ChildCaseClass.b", "integer", Some("int32"))
    assertRefIsAsExpected(actualSchemas, "ChildCaseClass.child", "#/components/schemas/ChildChildCaseClass")

    assertTypeAndFormatAreAsExpected(actualSchemas, "ParentCaseClass", "object")
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

  it should "make Map an OpenAPI object type with value schema as additionalProperties" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[Maps]()

    val actualSchemas = components.getSchemas

    assertTypeAndFormatAreAsExpected(actualSchemas, "Maps.a", "object")
    assertAdditionalPropertiesAreAsExpected(
      actualSchemas,
      "Maps.a",
      (new Schema).`type`("integer").format("int32")
    )
    assertTypeAndFormatAreAsExpected(actualSchemas, "Maps.b", "object")
    assertAdditionalPropertiesAreAsExpected(
      actualSchemas,
      "Maps.b",
      (new Schema).$ref("#/components/schemas/Arrays")
    )
  }

  it should "throw IllegalArgumentException if Map has key type different than String" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    intercept[IllegalArgumentException] {
      openAPIModelRegistration.register[WrongMaps]()
    }
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

  it should "make sealed type an OpenAPI schema (reference) with the oneOf attribute" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SumTypeClass]()

    val actualSchemas = components.getSchemas

    assertRefIsAsExpected(actualSchemas, "SumTypeClass.a", "#/components/schemas/SealedTrait")

    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedTraitVariant1.a", "string")
    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedTraitVariant2.a", "integer", Some("int32"))

    assertPredicateForPath(
      actualSchemas,
      "SealedTrait",
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

  it should "make sealed abstract class with case object an OpenAPI schema (reference) with the oneOf attribute" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[SumTypeClassWithSealedAbstractClass]()

    val actualSchemas = components.getSchemas

    assertRefIsAsExpected(
      actualSchemas,
      "SumTypeClassWithSealedAbstractClass.a",
      "#/components/schemas/SealedAbstractClass"
    )

    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedAbstractClassVariant.a", "string")
    assert(actualSchemas.containsKey("SealedAbstractClassCaseObject"))
    assertTypeAndFormatAreAsExpected(actualSchemas, "SealedAbstractClassCaseObject", "object")
    assert(Option(actualSchemas.get("SealedAbstractClassCaseObject").getProperties).isEmpty)

    assertPredicateForPath(
      actualSchemas,
      "SealedAbstractClass",
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

  it should "add discriminator to sealed types " +
    "and discriminator property to all children (if addDiscriminatorPropertyOnlyToDirectChildren is false)" +
    "if SumADTsShape is WithDiscriminator (with default DiscriminatorPropertyNameFn) in the config" in {
      val components = new Components
      val openAPIModelRegistration = new OpenAPIModelRegistration(
        components,
        config = OpenAPIModelRegistration.RegistrationConfig(
          sumADTsShape = OpenAPIModelRegistration.RegistrationConfig.SumADTsShape.WithDiscriminator(
            addDiscriminatorPropertyOnlyToDirectChildren = false
          )
        )
      )

      openAPIModelRegistration.register[NestedSealedTrait]()
      openAPIModelRegistration.register[NestedSealedTraitBreaker]()

      val actualSchemas = components.getSchemas

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTrait",
        schema => {
          val actualDiscriminator = schema.getDiscriminator
          val actualOneOf = schema.getOneOf.asScala
          val expectedOneOf = Seq(
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant1"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant2"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3")
          )

          val isPropertyNameCorrect = actualDiscriminator.getPropertyName === "nestedSealedTraitType"
          val isMappingEmpty = Option(actualDiscriminator.getMapping).map(_.isEmpty).getOrElse(true)
          val isOneOfCorrect = actualOneOf === expectedOneOf

          isPropertyNameCorrect && isMappingEmpty && isOneOfCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant1",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val areNonDiscriminatorPropertiesCorrect = actualProperties.contains("a") &&
            actualProperties.contains("b") &&
            actualProperties.contains("c") &&
            actualProperties("c").get$ref === "#/components/schemas/NestedSealedTrait"
          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant1")
          }
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 4

          areNonDiscriminatorPropertiesCorrect &&
          isDiscriminatorPropertyCorrect &&
          isDiscriminatorPropertyRequired &&
          isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant2",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant2")
          }
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 1

          isDiscriminatorPropertyCorrect && isDiscriminatorPropertyRequired && isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3",
        schema => {
          val actualDiscriminator = schema.getDiscriminator
          val actualOneOf = schema.getOneOf.asScala
          val expectedOneOf = Seq(
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3Subvariant1"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3Subvariant2")
          )

          val isPropertyNameCorrect = actualDiscriminator.getPropertyName === "nestedSealedTraitVariant3Type"
          val isMappingEmpty = Option(actualDiscriminator.getMapping).map(_.isEmpty).getOrElse(true)
          val isOneOfCorrect = actualOneOf === expectedOneOf
          val isParentDiscriminatorNotInProperties = Option(schema.getProperties).map(_.size == 0).getOrElse(true)

          isPropertyNameCorrect && isMappingEmpty && isOneOfCorrect && isParentDiscriminatorNotInProperties
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3Subvariant1",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val areNonDiscriminatorPropertiesCorrect = actualProperties.contains("a") &&
            actualProperties.contains("b") &&
            actualProperties("b").get$ref === "#/components/schemas/NestedSealedTrait"
          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitVariant3Type") && {
            val s = actualProperties("nestedSealedTraitVariant3Type")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3Subvariant1")
          }
          val isParentDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3")
          }
          val areDiscriminatorPropertyRequired = schema.getRequired.contains(
            "nestedSealedTraitVariant3Type"
          ) && schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 4

          areNonDiscriminatorPropertiesCorrect &&
          isDiscriminatorPropertyCorrect &&
          isParentDiscriminatorPropertyCorrect &&
          areDiscriminatorPropertyRequired &&
          isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3Subvariant2",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitVariant3Type") && {
            val s = actualProperties("nestedSealedTraitVariant3Type")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3Subvariant2")
          }
          val isParentDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3")
          }
          val areDiscriminatorPropertyRequired = schema.getRequired.contains(
            "nestedSealedTraitVariant3Type"
          ) && schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 2

          isDiscriminatorPropertyCorrect &&
          isParentDiscriminatorPropertyCorrect &&
          isCountOfPropertiesCorrect &&
          areDiscriminatorPropertyRequired
        }
      )

      assertTypeAndFormatAreAsExpected(actualSchemas, "NestedSealedTraitBreaker", "object")
      assertRefIsAsExpected(
        actualSchemas,
        "NestedSealedTraitBreaker.breaker",
        "#/components/schemas/NestedSealedTraitVariant1"
      )
    }

  it should "add discriminator to sealed types " +
    "and discriminator property to direct children (if addDiscriminatorPropertyOnlyToDirectChildren is true)" +
    "if SumADTsShape is WithDiscriminator (with default DiscriminatorPropertyNameFn) in the config" in {
      val components = new Components
      val openAPIModelRegistration = new OpenAPIModelRegistration(
        components,
        config = OpenAPIModelRegistration.RegistrationConfig(
          sumADTsShape = OpenAPIModelRegistration.RegistrationConfig.SumADTsShape.WithDiscriminator(
            addDiscriminatorPropertyOnlyToDirectChildren = true
          )
        )
      )

      openAPIModelRegistration.register[NestedSealedTrait]()

      val actualSchemas = components.getSchemas

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTrait",
        schema => {
          val actualDiscriminator = schema.getDiscriminator
          val actualOneOf = schema.getOneOf.asScala
          val expectedOneOf = Seq(
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant1"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant2"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3")
          )

          val isPropertyNameCorrect = actualDiscriminator.getPropertyName === "nestedSealedTraitType"
          val isMappingEmpty = Option(actualDiscriminator.getMapping).map(_.isEmpty).getOrElse(true)
          val isOneOfCorrect = actualOneOf === expectedOneOf

          isPropertyNameCorrect && isMappingEmpty && isOneOfCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant1",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val areNonDiscriminatorPropertiesCorrect = actualProperties.contains("a") &&
            actualProperties.contains("b") &&
            actualProperties.contains("c") &&
            actualProperties("c").get$ref === "#/components/schemas/NestedSealedTrait"
          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant1")
          }
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 4

          areNonDiscriminatorPropertiesCorrect &&
          isDiscriminatorPropertyCorrect &&
          isDiscriminatorPropertyRequired &&
          isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant2",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitType") && {
            val s = actualProperties("nestedSealedTraitType")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant2")
          }
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitType")
          val isCountOfPropertiesCorrect = actualProperties.size === 1

          isDiscriminatorPropertyCorrect && isDiscriminatorPropertyRequired && isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3",
        schema => {
          val actualDiscriminator = schema.getDiscriminator
          val actualOneOf = schema.getOneOf.asScala
          val expectedOneOf = Seq(
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3Subvariant1"),
            new Schema().$ref("#/components/schemas/NestedSealedTraitVariant3Subvariant2")
          )

          val isPropertyNameCorrect = actualDiscriminator.getPropertyName === "nestedSealedTraitVariant3Type"
          val isMappingEmpty = Option(actualDiscriminator.getMapping).map(_.isEmpty).getOrElse(true)
          val isOneOfCorrect = actualOneOf === expectedOneOf
          val isParentDiscriminatorNotInProperties = Option(schema.getProperties).map(_.size == 0).getOrElse(true)

          isPropertyNameCorrect && isMappingEmpty && isOneOfCorrect && isParentDiscriminatorNotInProperties
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3Subvariant1",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val areNonDiscriminatorPropertiesCorrect = actualProperties.contains("a") &&
            actualProperties.contains("b") &&
            actualProperties("b").get$ref == "#/components/schemas/NestedSealedTrait"
          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitVariant3Type") && {
            val s = actualProperties("nestedSealedTraitVariant3Type")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3Subvariant1")
          }
          val isParentDiscriminatorNotInProperties = !actualProperties.contains("nestedSealedTraitType")
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitVariant3Type")
          val isCountOfPropertiesCorrect = actualProperties.size === 3

          areNonDiscriminatorPropertiesCorrect &&
          isDiscriminatorPropertyCorrect &&
          isParentDiscriminatorNotInProperties &&
          isDiscriminatorPropertyRequired &&
          isCountOfPropertiesCorrect
        }
      )

      assertPredicateForPath(
        actualSchemas,
        "NestedSealedTraitVariant3Subvariant2",
        schema => {
          val actualProperties = schema.getProperties.asScala

          val isDiscriminatorPropertyCorrect = actualProperties.contains("nestedSealedTraitVariant3Type") && {
            val s = actualProperties("nestedSealedTraitVariant3Type")
            s.getType === "string" && s.getEnum.asScala === Seq("NestedSealedTraitVariant3Subvariant2")
          }
          val isParentDiscriminatorNotInProperties = !actualProperties.contains("nestedSealedTraitType")
          val isDiscriminatorPropertyRequired = schema.getRequired.contains("nestedSealedTraitVariant3Type")
          val isCountOfPropertiesCorrect = actualProperties.size === 1

          isDiscriminatorPropertyCorrect &&
          isParentDiscriminatorNotInProperties &&
          isCountOfPropertiesCorrect &&
          isDiscriminatorPropertyRequired
        }
      )
    }

  it should "not fail for empty sealed trait" in {
    val components = new Components
    val openAPIModelRegistration = new OpenAPIModelRegistration(components)

    openAPIModelRegistration.register[EmptySealedTraitClass]()

    val actualSchemas = components.getSchemas

    assertRefIsAsExpected(
      actualSchemas,
      "EmptySealedTraitClass.a",
      "#/components/schemas/EmptySealedTrait"
    )

    assertPredicateForPath(
      actualSchemas,
      "EmptySealedTrait",
      schema => {
        val actualOneOf = schema.getOneOf.asScala
        val expectedOneOf = Seq()
        actualOneOf === expectedOneOf
      }
    )
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
    val openAPIModelRegistration = new OpenAPIModelRegistration(
      components,
      extraTypesHandler = extraTypesHandler
    )

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
      val openAPIModelRegistration = new OpenAPIModelRegistration(
        components,
        extraTypesHandler = extraTypesHandler
      )

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

  private def assertAdditionalPropertiesAreAsExpected(
    actualSchemas: java.util.Map[String, Schema[_]],
    fieldPath: String,
    expectedAdditionalProperties: AnyRef
  ): scalatest.Assertion = assertPredicateForPath(
    actualSchemas,
    fieldPath,
    schema => Option(schema.getAdditionalProperties).map(_ === expectedAdditionalProperties).getOrElse(false)
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

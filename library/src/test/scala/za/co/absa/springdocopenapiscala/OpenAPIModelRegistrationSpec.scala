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

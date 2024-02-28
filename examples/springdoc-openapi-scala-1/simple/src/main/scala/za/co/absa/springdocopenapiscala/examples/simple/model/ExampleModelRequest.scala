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

package za.co.absa.springdocopenapiscala.examples.simple.model

import com.fasterxml.jackson.databind.JsonNode

case class ExampleModelRequest(
  a: Int,
  b: String,
  c: Option[Int],
  d: JsonNode,
  e: ExampleModelRequest.Expression,
  f: Option[ExampleModelRequest.Expression]
)

object ExampleModelRequest {

  sealed trait Expression

  object Expression {
    case object Something extends Expression
    sealed trait Literal extends Expression

    object Literal {
      case class StringLiteral(value: String) extends Literal
      case class IntLiteral(value: String) extends Literal
    }
  }

}

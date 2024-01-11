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

package za.co.absa.springdocopenapiscala.examples.simple

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springframework.context.annotation.{Bean, Configuration}
import za.co.absa.springdocopenapiscala.{Bundle, OpenAPIModelRegistration}

import scala.reflect.runtime.universe.typeOf

@Configuration
class OpenAPIConfiguration {

  private val springDocOpenAPIScalaBundle = new Bundle(
    Seq((openAPI: OpenAPI) =>
      openAPI.setInfo(
        new Info()
          .title("Example API with springdoc-openapi v1.x")
          .version("1.0.0")
      )
    ),
    OpenAPIModelRegistration.ExtraTypesHandling.simpleMapping {
      case t if t =:= typeOf[JsonNode] =>
        val schema = new Schema
        schema.setType("string")
        schema.setFormat("json")
        schema
    }
  )

  @Bean
  def openAPICustomizer: OpenApiCustomiser = springDocOpenAPIScalaBundle.customizer

  @Bean
  def openAPIModelRegistration: OpenAPIModelRegistration = springDocOpenAPIScalaBundle.modelRegistration

}

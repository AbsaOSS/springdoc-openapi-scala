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

import io.swagger.v3.oas.models.{Components, OpenAPI}
import za.co.absa.springdocopenapiscala.OpenAPIModelRegistration.{ExtraTypesHandling, RegistrationConfig}
import za.co.absa.springdocopenapiscala.SpringdocOpenAPIVersionSpecificTypes._

/**
 *  Glues all components of `springdoc-openapi-scala` together
 *  and enables additional customization (for example to set info or support custom types).
 *
 *  @param extraOpenAPICustomizers additional customizers that are executed after [[OpenAPIScalaCustomizer]]
 *  @param extraTypesHandler [[ExtraTypesHandling.ExtraTypesHandler]] to be used in model registration
 */
class Bundle(
  extraOpenAPICustomizers: Seq[OpenApiCustomizer] = Seq.empty,
  extraTypesHandler: ExtraTypesHandling.ExtraTypesHandler = ExtraTypesHandling.noExtraHandling,
  registrationConfig: RegistrationConfig = RegistrationConfig()
) {

  private val components = new Components

  val modelRegistration: OpenAPIModelRegistration = new OpenAPIModelRegistration(
    components,
    extraTypesHandler,
    registrationConfig
  )

  val customizer: OpenApiCustomizer = {
    val openAPIScalaCustomizer = new OpenAPIScalaCustomizer(components)

    (openApi: OpenAPI) => (openAPIScalaCustomizer +: extraOpenAPICustomizers).foreach(_.customise(openApi))
  }

}

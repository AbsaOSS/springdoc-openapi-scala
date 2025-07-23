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
import za.co.absa.springdocopenapiscala.SpringdocOpenAPIVersionSpecificTypes.OpenApiCustomizer

import scala.collection.JavaConverters._

class OpenAPIScalaCustomizer(components: Components) extends OpenApiCustomizer {

  override def customise(openAPIOutOfSync: OpenAPI): Unit = {
    // This is needed as for some reason springdoc-openapi cache the `OpenAPI` at the beginning
    // and newly added `Components` are not taken into account on JSON/YAML generation.
    openAPIOutOfSync.setComponents(components)

    fixResponsesReturningUnit(openAPIOutOfSync)
  }

  private def fixResponsesReturningUnit(openAPI: OpenAPI): Unit = {
    for {
      paths <- Option(openAPI.getPaths)
      (_, path) <- paths.asScala
      (_, operation) <- path.readOperationsMap.asScala
      (_, response) <- operation.getResponses.asScala
      content <- Option(response.getContent)
      (_, mediaType) <- content.asScala
      schema <- Option(mediaType.getSchema)
      if schema.get$ref == "#/components/schemas/BoxedUnit"
    } response.setContent(None.orNull)
  }
}

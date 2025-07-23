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

import org.scalatest.flatspec.AnyFlatSpec

import io.swagger.v3.oas.models.media.{Content, MediaType, Schema}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models._

class OpenAPIScalaCustomizerSpec extends AnyFlatSpec {

  def initializeOpenAPI: OpenAPI = new OpenAPI()
    .paths(
      new Paths()
        .addPathItem(
          "/api/endpoint",
          new PathItem()
            .post(
              new Operation()
                .responses(
                  new ApiResponses()
                    .addApiResponse(
                      "200",
                      new ApiResponse()
                        .content(
                          new Content()
                            .addMediaType(
                              "*/*",
                              new MediaType()
                                .schema(
                                  new Schema()
                                    .$ref("#/components/schemas/BoxedUnit")
                                )
                            )
                        )
                    )
                )
            )
        )
    )

  behavior of "customise"

  it should "set `components` of its argument OpenAPI object to one injected via DI to the class" in {
    val components = new Components().addSchemas("a", new Schema)
    val openAPIScalaCustomizer = new OpenAPIScalaCustomizer(components)

    val openAPI = initializeOpenAPI

    openAPIScalaCustomizer.customise(openAPI)

    assert(openAPI.getComponents === components)
  }

  it should "convert all responses returning Unit (BoxedUnit reference) to empty response" in {
    val components = new Components()
    val openAPIScalaCustomizer = new OpenAPIScalaCustomizer(components)

    val openAPI = initializeOpenAPI

    openAPIScalaCustomizer.customise(openAPI)

    assert(
      Option(openAPI.getPaths.get("/api/endpoint").getPost.getResponses.get("200").getContent).isEmpty
    )
  }

  it should "do nothing if a response doesn't have content" in {
    val components = new Components()
    val openAPIScalaCustomizer = new OpenAPIScalaCustomizer(components)

    val openAPI = new OpenAPI()
      .paths(
        new Paths()
          .addPathItem(
            "/api/endpoint",
            new PathItem()
              .delete(
                new Operation()
                  .responses(
                    new ApiResponses()
                      .addApiResponse(
                        "204",
                        new ApiResponse()
                          .description("No Content")
                      )
                  )
              )
          )
      )

    openAPIScalaCustomizer.customise(openAPI)

    assert(
      Option(openAPI.getPaths.get("/api/endpoint").getDelete.getResponses.get("204").getContent).isEmpty
    )
  }

  it should "do nothing if a response doesn't have a schema" in {
    val components = new Components()
    val openAPIScalaCustomizer = new OpenAPIScalaCustomizer(components)

    val openAPI = new OpenAPI()
      .paths(
        new Paths()
          .addPathItem(
            "/api/endpoint",
            new PathItem()
              .delete(
                new Operation()
                  .responses(
                    new ApiResponses()
                      .addApiResponse(
                        "204",
                        new ApiResponse()
                          .content(
                            new Content()
                              .addMediaType(
                                "*/*",
                                new MediaType()
                              )
                          )
                      )
                  )
              )
          )
      )

    openAPIScalaCustomizer.customise(openAPI)

    assert(
      Option(openAPI.getPaths.get("/api/endpoint").getDelete.getResponses.get("204").getContent).isDefined
    )
  }

}

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

package za.co.absa.springdocopenapiscala.examples.simple.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.{PostMapping, RequestBody, RequestMapping, RestController}

import za.co.absa.springdocopenapiscala.OpenAPIModelRegistration
import za.co.absa.springdocopenapiscala.examples.simple.model.{ExampleModelRequest, ExampleModelResponse}

import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping(
  value = Array("/api/v1/example")
)
class ExampleController @Autowired()(openAPIModelRegistration: OpenAPIModelRegistration) {

  openAPIModelRegistration.register[ExampleModelRequest]()
  openAPIModelRegistration.register[ExampleModelResponse]()

  @PostMapping(
    value = Array("/some-endpoint"),
    produces = Array(MediaType.APPLICATION_JSON_VALUE)
  )
  def someEndpoint(@RequestBody body: ExampleModelRequest): CompletableFuture[ExampleModelResponse] = {
    val response = ExampleModelResponse(Seq(body.a, 123), true)

    CompletableFuture.completedFuture(response)
  }

}

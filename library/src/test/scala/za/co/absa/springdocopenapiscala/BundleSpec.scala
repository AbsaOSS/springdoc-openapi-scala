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

import io.swagger.v3.oas.models.OpenAPI
import org.mockito.Mockito.{mock, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import za.co.absa.springdocopenapiscala.SpringdocOpenAPIVersionSpecificTypes.OpenApiCustomizer

class BundleSpec extends AnyFlatSpec with Matchers {

  it should "instantiate the class correctly" in {
    val bundle = new Bundle()
    val openAPI = new OpenAPI()

    bundle.customizer shouldBe a[OpenApiCustomizer]
    bundle.modelRegistration shouldBe a[OpenAPIModelRegistration]

    bundle.customizer.customise(openAPI)
  }

  it should "use extra customizers" in {
    val openApiCustomizer = mock(classOf[OpenApiCustomizer])
    val bundle = new Bundle(Seq(openApiCustomizer))
    val openAPI = new OpenAPI()

    bundle.customizer.customise(openAPI)

    verify(openApiCustomizer).customise(openAPI)
  }
}

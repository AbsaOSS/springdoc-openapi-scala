# springdoc-openapi-scala

An enhancement to [springdoc-openapi](https://github.com/springdoc/springdoc-openapi) that adds better support for Scala.

## Motivation
Scala isn't well-supported in springdoc-openapi by default, for example:
- `case class` parameters are not recognized by default, one has to add something like `@BeanProperty` to each
- even with `@BeanProperty`, most parameters with generic type (like `Option`) don't work correctly
- even with `@BeanProperty`, all parameters are marked as not required in generated OpenAPI docs
- Spring endpoints returning `Unit` are not "No Content" but instead show that the endpoint returns `BoxedUnit`

One option to overcome these limitations is to annotate the model with annotations provided by springdoc-openapi.
But even with them, `@BeanPropery` or equivalent must be added to each case class parameter.

This library aims to avoid pollution of the model by custom annotations and dependency on Spring related libraries.

## Features
- all parameters of a `case class` are automatically recognized without any custom annotations
- all parameters of a `case class` that have a type different from `Option` are marked as required
- Spring endpoints returning Unit are "No Content"
- support for basic Scala collections (`Map`, `Seq`, `Set`, `Array`) as types of `case class` parameters
- only top-level case classes need to be registered, child case classes are then recursively registered

## Usage

### Provided dependencies
The library has `"org.springdoc" % "springdoc-openapi-webmvc-core"` as a provided dependency, 
thus users of the library have to include that dependency in their projects.
Versions `1.6.7` up to `1.7.0` (including) are supported.

### Add library dependency to SBT/Maven
SBT:
```sbt
libraryDependencies ++= Seq("za.co.absa" %% "springdoc-openapi-scala" % VERSION)
```

Maven:
```xml
<dependency>
   <groupId>za.co.absa</groupId>
   <artifactId>springdoc-openapi-scala_{scala_binary_version}</artifactId>
   <version>${version}</version>
</dependency>
```
where `scala_binary_version` is either `2.12` or `2.13`.

### Create custom OpenAPI Spring Configuration
```scala
@Configuration
class OpenAPIConfiguration {

  private val springDocOpenAPIScalaBundle = new Bundle(
    Seq((openAPI: OpenAPI) =>
      openAPI.setInfo(
        new Info()
          .title("Example API")
          .version("1.0.0")
      )
    )
  )

  @Bean
  def openAPICustomizer: OpenApiCustomiser = springDocOpenAPIScalaBundle.customizer

  @Bean
  def openAPIModelRegistration: OpenAPIModelRegistration = springDocOpenAPIScalaBundle.modelRegistration

}
```

### Register top-level model case classes (for example in Controller)
Example model:
```scala
case class ExampleModelRequest(a: Int, b: String, c: Option[Int])

case class ExampleModelResponse(d: Seq[Int], e: Boolean)
```

can be registered for example in `Controller`:

```scala
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
    ...
  }
  
}
```

## Examples

### Simple example
Can be found in this repo: [link](examples/simple). It generates the following OpenAPI JSON doc:
```json
{
    "openapi": "3.0.1",
    "info": {
        "title": "Example API",
        "version": "1.0.0"
    },
    "servers": [
        {
            "url": "http://localhost:8080",
            "description": "Generated server url"
        }
    ],
    "paths": {
        "/api/v1/example/some-endpoint": {
            "post": {
                "tags": [
                    "example-controller"
                ],
                "operationId": "someEndpoint",
                "requestBody": {
                    "content": {
                        "application/json": {
                            "schema": {
                                "$ref": "#/components/schemas/ExampleModelRequest"
                            }
                        }
                    },
                    "required": true
                },
                "responses": {
                    "200": {
                        "description": "OK",
                        "content": {
                            "application/json": {
                                "schema": {
                                    "$ref": "#/components/schemas/ExampleModelResponse"
                                }
                            }
                        }
                    }
                }
            }
        }
    },
    "components": {
        "schemas": {
            "ExampleModelRequest": {
                "required": [
                    "a",
                    "b"
                ],
                "properties": {
                    "a": {
                        "type": "integer",
                        "format": "int32"
                    },
                    "b": {
                        "type": "string"
                    },
                    "c": {
                        "type": "integer",
                        "format": "int32"
                    }
                }
            },
            "ExampleModelResponse": {
                "required": [
                    "d",
                    "e"
                ],
                "properties": {
                    "d": {
                        "type": "array",
                        "items": {
                            "type": "integer",
                            "format": "int32"
                        }
                    },
                    "e": {
                        "type": "boolean"
                    }
                }
            }
        }
    }
}
```

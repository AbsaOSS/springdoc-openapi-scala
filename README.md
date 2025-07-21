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
- support for Scala `Enumeration` where simple `Value` constructor is used (without `name`)
- support for sum ADTs (`sealed trait` and `sealed abstract class`) with optional discriminator

## Usage

springdoc-openapi-scala supports two major versions of springdoc-openapi: 1.x and 2.x.

### Provided dependencies
The library has springdoc-openapi as a provided dependency, 
thus users of the library have to include that dependency in their projects:
- for springdoc-openapi 1.x versions `1.6.7` up to `1.7.0` (including) of 
`"org.springdoc" % "springdoc-openapi-webmvc-core"` are supported
- for springdoc-openapi 2.x versions `2.0.0` up to `2.8.9` (including) of
`"org.springdoc" % "springdoc-openapi-starter-webmvc-api"` are supported

### Add library dependency to SBT/Maven
#### SBT
If you want to use the library with springdoc-openapi 1.x, add:
```sbt
libraryDependencies ++= Seq("za.co.absa" %% "springdoc-openapi-scala-1" % VERSION)
```
if with springdoc-openapi 2.x, add:
```sbt
libraryDependencies ++= Seq("za.co.absa" %% "springdoc-openapi-scala-2" % VERSION)
```

#### Maven
If you want to use the library with springdoc-openapi 1.x, add:
```xml
<dependency>
   <groupId>za.co.absa</groupId>
   <artifactId>springdoc-openapi-scala-1_${scala_binary_version}</artifactId>
   <version>${version}</version>
</dependency>
```
if with springdoc-openapi 2.x, add:
```xml
<dependency>
   <groupId>za.co.absa</groupId>
   <artifactId>springdoc-openapi-scala-2_${scala_binary_version}</artifactId>
   <version>${version}</version>
</dependency>
```

where `scala_binary_version` is either `2.12` or `2.13`.

### Create custom OpenAPI Spring Configuration
For springdoc-openapi 1.x
```scala
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springframework.context.annotation.{Bean, Configuration}

import za.co.absa.springdocopenapiscala.{Bundle, OpenAPIModelRegistration}

@Configuration
class OpenAPIConfiguration {

  private val springDocOpenAPIScalaBundle = new Bundle(
    Seq((openAPI: OpenAPI) =>
      openAPI.setInfo(
        new Info()
          .title("Example API with springdoc-openapi v1.x")
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

For springdoc-openapi 2.x
```scala
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.{Bean, Configuration}

import za.co.absa.springdocopenapiscala.{Bundle, OpenAPIModelRegistration}

@Configuration
class OpenAPIConfiguration {

  private val springDocOpenAPIScalaBundle = new Bundle(
    Seq((openAPI: OpenAPI) =>
      openAPI.setInfo(
        new Info()
          .title("Example API with springdoc-openapi v2.x")
          .version("1.0.0")
      )
    )
  )

  @Bean
  def openAPICustomizer: OpenApiCustomizer = springDocOpenAPIScalaBundle.customizer

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

### Adding support for custom types
To add support for custom types (or overwrite handling of any type supported by the library) 
one can create a custom `ExtraTypesHandler` and provide it when creating a `Bundle`.

There are multiple ways to do so, the simplest is to use `ExtraTypesHandling.simpleMapping`, for example:
```scala
OpenAPIModelRegistration.ExtraTypesHandling.simpleMapping {
  case t if t =:= typeOf[JsValue] =>
    val schema = new Schema
    schema.setType("string")
    schema.setFormat("json")
    schema
}
```
This `ExtraTypesHandler` handles `JsValue` by mapping it to simple OpenAPI `string` type with `json` format.

But `ExtraTypesHandler` can also be much more powerful, for example: 
```scala
case class CustomClassComplexChild(a: Option[Int])

class CustomClass(val complexChild: CustomClassComplexChild) {
  // these won't be included
  val meaningOfLife: Int = 42
  val alphabetHead: String = "abc"
}

...

val extraTypesHandler: ExtraTypesHandler = (tpe: Type) =>
  tpe match {
    case t if t =:= typeOf[CustomClass] =>
      val childTypesToBeResolvedByTheLibrary = Set(typeOf[CustomClassComplexChild])
      
      val handleFn: HandleFn = (resolvedChildTypes, context) => {
        val name = "CustomClass"
        val customClassComplexChildResolvedSchema = resolvedChildTypes(typeOf[CustomClassComplexChild])
        val schema = new Schema
        schema.addProperty("complexChild", customClassComplexChildResolvedSchema)
        context.components.addSchemas(name, schema)
        val schemaReference = new Schema
        schemaReference.set$ref(s"#/components/schemas/$name")
        schemaReference
      }
      
      (childTypesToBeResolvedByTheLibrary, handleFn)
  }
```
This `ExtraTypesHandler` handles `CustomClass`. 
`CustomClass` uses `CustomClassComplexChild`, 
so the handler requests the library to resolve its type (`childTypesToBeResolvedByTheLibrary`).
This resolved type is available as input to `HandleFn`.
Then, in `handleFn`, the handler creates a `Schema` object for `CustomClass`, 
adds it to `Components` so that it can be referenced by name `CustomClass`,
and returns reference to that object.

### Registration configuration
It is possible to further customize registration by providing custom `RegistrationConfig` to `OpenAPIModelRegistration`.

#### Example
```scala
val components = ...
val registration = OpenAPIModelRegistration(
  components,
  config = RegistrationConfig(
    OpenAPIModelRegistration.RegistrationConfig(
      sumADTsShape =
         // default values apply for discriminatorPropertyNameFn, addDiscriminatorPropertyOnlyToDirectChildren
         OpenAPIModelRegistration.RegistrationConfig.SumADTsShape.WithDiscriminator()
    )
  )
)
```

#### sumADTsShape
This config property sets how sum ADTs are registered. It has two possible values:
- `RegistrationConfig.SumADTsShape.WithoutDiscriminator` - default option, doesn't add discriminators
- `RegistrationConfig.SumADTsShape.WithDiscriminator(discriminatorPropertyNameFn, addDiscriminatorPropertyOnlyToDirectChildren)` - 
   adds discriminator to sealed types schema,
   and also adds discriminator to sum ADTs elements properties; discriminator property name is customizable by `discriminatorPropertyNameFn`,
   by default it takes sealed type name, converts its first letter to lower case, and adds `"Type"` suffix,
   for example if sealed type name is `Expression`, the property name is `expressionType`;
   if `addDiscriminatorPropertyOnlyToDirectChildren` is `false`, discriminator property is added to all children,
   so for example in `ADT = A | B | C; B = D | E` discriminator of `ADT` would be added to `A`, `C`, `D`, `E`
   (`D` and `E` would have discriminator of `B` in addition to that) 
   while with  `addDiscriminatorPropertyOnlyToDirectChildren` set to `true` (default) 
   it would be added only to `A` and `C`

## Examples

### Simple example for springdoc-openapi-scala-1
Can be found in this repo: [link](examples/springdoc-openapi-scala-1/simple). It generates the following OpenAPI JSON doc:
```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example API with springdoc-openapi v1.x",
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
      "IntLiteral": {
        "required": [
          "value"
        ],
        "properties": {
          "value": {
            "type": "string"
          }
        }
      },
      "StringLiteral": {
        "required": [
          "value"
        ],
        "properties": {
          "value": {
            "type": "string"
          }
        }
      },
      "Literal": {
        "oneOf": [
          {
            "$ref": "#/components/schemas/IntLiteral"
          },
          {
            "$ref": "#/components/schemas/StringLiteral"
          }
        ]
      },
      "Something": {},
      "Expression": {
        "oneOf": [
          {
            "$ref": "#/components/schemas/Literal"
          },
          {
            "$ref": "#/components/schemas/Something"
          }
        ]
      },
      "ExampleModelRequest": {
        "required": [
          "a",
          "b",
          "d",
          "e"
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
          },
          "d": {
            "type": "string",
            "format": "json"
          },
          "e": {
            "$ref": "#/components/schemas/Expression"
          },
          "f": {
            "$ref": "#/components/schemas/Expression"
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

### Simple example for springdoc-openapi-scala-2
Can be found in this repo: [link](examples/springdoc-openapi-scala-2/simple). It generates the following OpenAPI JSON doc:
```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example API with springdoc-openapi v2.x",
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
    },
    "/api/v1/example/empty-endpoint": {
      "post": {
        "tags": [
          "example-controller"
        ],
        "operationId": "emptyEndpoint",
        "responses": {
          "204": {
            "description": "No Content"
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
          "b",
          "d",
          "e"
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
          },
          "d": {
            "type": "string",
            "enum": [
              "OptionC",
              "OptionB",
              "OptionA"
            ]
          },
          "e": {
            "type": "string",
            "format": "json"
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

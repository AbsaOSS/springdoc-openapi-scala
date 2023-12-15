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
Currently only version `1.7.0` is supported.

### Add library dependency to SBT/Maven
SBT:
```sbt
libraryDependencies ++= Seq("za.co.absa" %% "springdoc-openapi-scala" % VERSION)
```

Maven:
```xml
<dependency>
   <groupId>za.co.absa</groupId>
   <artifactId>springdoc-openapi-scala_2.12</artifactId>
   <version>${version}</version>
</dependency>
```

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

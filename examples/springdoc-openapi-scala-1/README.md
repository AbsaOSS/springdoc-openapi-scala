# springdoc-openapi-scala-2 Example Application

This is a simple example application demonstrating the use of springdoc-openapi-scala with Spring Boot 2.x.

## Prerequisites

- JDK 17 or higher
- SBT 1.x
- Scala 2.12.x

## Running the Application

To run the application with hot-reload enabled:

```bash
cd springdoc-openapi-scala/examples/springdoc-openapi-scala-1/simple
sbt compile
sbt "~reStart"
```

The `~reStart` command will automatically restart the application when source files change.

## Accessing the API Documentation

Once the application is running, you can access the OpenAPI documentation at:

- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Features Demonstrated

- Integration of springdoc-openapi with Scala classes
- Automatic schema generation for Scala case classes
- Support for complex type hierarchies
- Custom schema customization

## Project Structure

- `src/main/scala/.../Application.scala` - Spring Boot application entry point
- `src/main/scala/.../OpenAPIConfiguration.scala` - OpenAPI configuration
- `src/main/scala/.../controller/` - REST controllers
- `src/main/scala/.../model/` - Data models
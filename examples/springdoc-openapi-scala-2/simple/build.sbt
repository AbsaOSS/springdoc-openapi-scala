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

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "za.co.absa" %% "springdoc-openapi-scala-2" % "0.2.0-SNAPSHOT",
      "org.springdoc" % "springdoc-openapi-starter-webmvc-api" % "2.0.0",
      "org.springframework.boot" % "spring-boot-starter-web" % "3.0.4"
    ),
    webappWebInfClasses := true,
    inheritJarManifest := true
  )
  .enablePlugins(TomcatPlugin)
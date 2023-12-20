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

import Dependencies._
import com.github.sbt.jacoco.report.JacocoReportSettings

ThisBuild / organization := "za.co.absa"
ThisBuild / scalaVersion := Versions.scala212
ThisBuild / versionScheme := Some("early-semver")

lazy val supportedScalaVersions = List(Versions.scala212, Versions.scala213)

lazy val commonJacocoReportSettings: JacocoReportSettings = JacocoReportSettings(
  formats = Seq(JacocoReportFormats.HTML, JacocoReportFormats.XML)
)

lazy val commonJacocoExcludes: Seq[String] = Seq(
//  "za.co.absa.springdocopenapiscala.*"
)

lazy val root = (project in file("."))
  .settings(
    name := "springdoc-openapi-scala",
    libraryDependencies ++= libraryDependencyList(scalaVersion.value),
    crossScalaVersions := supportedScalaVersions
  ).settings(
    jacocoReportSettings := commonJacocoReportSettings.withTitle(s"SpringDoc OpenApi - scala:${scalaVersion.value}"),
    jacocoExcludes := commonJacocoExcludes
  )

lazy val simpleExample = (project in file("examples/simple"))
  .settings(
    libraryDependencies ++= exampleProjectsDependencyList,
    webappWebInfClasses := true,
    inheritJarManifest := true
  )
  .enablePlugins(TomcatPlugin)
  .dependsOn(root)

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

import sbt._

object Dependencies {

  object Versions {
    val scala212 = "2.12.18"
    val scala213 = "2.13.12"

    val scalatest = "3.2.15"

    val springdocOpenapi = "1.7.0"
  }

  def dependencyList(scalaVersion: String, springdocVersion: String): Seq[ModuleID] = {
    List(
      "org.scala-lang" % "scala-reflect" % scalaVersion,
      "org.springdoc" % "springdoc-openapi-webmvc-core" % springdocVersion % Provided,
      "org.scalatest" %% "scalatest" % Versions.scalatest % Test,
      "org.scalatest" %% "scalatest-flatspec" % Versions.scalatest % Test
    )
  }

}

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

ThisBuild / organizationHomepage := Some(url("https://www.absa.africa"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/AbsaOSS/springdoc-openapi-scala/tree/main"),
    connection = "scm:git:git://github.com/AbsaOSS/springdoc-openapi-scala.git",
    devConnection = "scm:git:ssh://github.com/AbsaOSS/springdoc-openapi-scala.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "jakipatryk",
    name  = "Bartlomiej Baj",
    email = "bartlomiej.baj@absa.africa",
    url   = url("https://github.com/jakipatryk")
  ),
  Developer(
    id    = "kevinwallimann",
    name  = "Kevin Wallimann",
    email = "kevin.wallimann@absa.africa",
    url   = url("https://github.com/kevinwallimann")
  ),
  Developer(
    id    = "AlexGuzmanAtAbsa",
    name  = "Alejandro Guzman",
    email = "alex.guzman@absa.africa",
    url   = url("https://github.com/AlexGuzmanAtAbsa")
  )
)

ThisBuild / homepage := Some(url("https://github.com/AbsaOSS/springdoc-openapi-scala"))
ThisBuild / description := "Enhancement of springdoc-openapi for Scala"
ThisBuild / organizationName := "ABSA Group Limited"
ThisBuild / startYear := Some(2023)
ThisBuild / licenses += "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")

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

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.1")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.2")

addSbtPlugin("org.jetbrains.scala" % "sbt-ide-settings" % "1.1.1")
addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "4.2.4")

// sbt-jacoco - workaround related dependencies required to download
lazy val ow2Version = "9.5"
lazy val jacocoVersion = "0.8.11-absa.1"
val sbtJacocoVersion = "3.4.1-absa.4"
val scalaArmVersion = "2.0"

def jacocoUrl(artifactName: String): String = s"https://github.com/AbsaOSS/jacoco/releases/download/$jacocoVersion/org.jacoco.$artifactName-$jacocoVersion.jar"
def ow2Url(artifactName: String): String = s"https://repo1.maven.org/maven2/org/ow2/asm/$artifactName/$ow2Version/$artifactName-$ow2Version.jar"
def armUrl(scalaMajor: String): String = s"https://repo1.maven.org/maven2/com/jsuereth/scala-arm_$scalaMajor/$scalaArmVersion/scala-arm_$scalaMajor-$scalaArmVersion.jar"

addSbtPlugin("com.jsuereth" %% "scala-arm" % scalaArmVersion from armUrl("2.11"))
addSbtPlugin("com.jsuereth" %% "scala-arm" % scalaArmVersion from armUrl("2.12"))

addSbtPlugin("za.co.absa.jacoco" % "report" % jacocoVersion from jacocoUrl("report"))
addSbtPlugin("za.co.absa.jacoco" % "core" % jacocoVersion from jacocoUrl("core"))
addSbtPlugin("za.co.absa.jacoco" % "agent" % jacocoVersion from jacocoUrl("agent"))
addSbtPlugin("org.ow2.asm" % "asm" % ow2Version from ow2Url("asm"))
addSbtPlugin("org.ow2.asm" % "asm-commons" % ow2Version from ow2Url("asm-commons"))
addSbtPlugin("org.ow2.asm" % "asm-tree" % ow2Version from ow2Url("asm-tree"))

addSbtPlugin("za.co.absa.sbt" % "sbt-jacoco" % sbtJacocoVersion from s"https://github.com/AbsaOSS/sbt-jacoco/releases/download/$sbtJacocoVersion/sbt-jacoco-$sbtJacocoVersion.jar")

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
import sbt.Keys._
import sbt.internal.ProjectMatrix
import Dependencies._
import JacocoSetup._
import com.github.sbt.jacoco.JacocoKeys.{jacocoExcludes, jacocoReportSettings}

case class SpringdocOpenAPIVersionAxis(springdocOpenAPIMajorVersion: Int) extends sbt.VirtualAxis.WeakAxis {
  override val directorySuffix = s"-$springdocOpenAPIMajorVersion"
  override val idSuffix: String = s"_$springdocOpenAPIMajorVersion"
}

object SpringdocOpenAPIVersionAxis {

  implicit class ProjectExtension(val projectMatrix: ProjectMatrix) extends AnyVal {

    def row(
      axis: SpringdocOpenAPIVersionAxis,
      scalaVersions: Seq[String],
      settings: Def.SettingsDefinition*
    ): ProjectMatrix = {
      val springdocOpenAPIMajorVersion = axis.springdocOpenAPIMajorVersion

      scalaVersions.foldLeft(projectMatrix) { case (currentProjectMatrix, scalaVersion) =>
        currentProjectMatrix.customRow(
          scalaVersions = Seq(scalaVersion),
          axisValues = Seq(axis, VirtualAxis.jvm),
          _.settings(
            moduleName := name.value + axis.directorySuffix,
            libraryDependencies ++= libraryDependencyList(springdocOpenAPIMajorVersion, scalaVersion),
            jacocoReportSettings := jacocoSettings(springdocOpenAPIMajorVersion, scalaVersion),
            jacocoExcludes := jacocoProjectExcludes(springdocOpenAPIMajorVersion, scalaVersion)
          ).settings(settings: _*)
        )
      }
    }

  }

}

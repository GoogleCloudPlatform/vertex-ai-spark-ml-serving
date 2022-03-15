/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Dependencies._

ThisBuild / scalaVersion     := "2.12.14"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.google.cloud"
ThisBuild / organizationName := "Google"

ThisBuild / assemblyMergeStrategy := {
  case PathList(ps @ _*) if ps.contains("module-info.class") =>
    MergeStrategy.concat
  case PathList("META-INF", "spring-configuration-metadata.json") =>
    MergeStrategy.concat
  case PathList("META-INF", "additional-spring-configuration-metadata.json") =>
    MergeStrategy.concat
  case PathList("META-INF", "spring.handlers")  => MergeStrategy.concat
  case PathList("META-INF", "spring.schemas")   => MergeStrategy.concat
  case PathList("META-INF", "spring.factories") => MergeStrategy.concat
  case PathList("META-INF", "web-fragment.xml") => MergeStrategy.concat
  case PathList("META-INF", "spring-autoconfigure-metadata.properties") =>
    MergeStrategy.concat
  case PathList("org", "apache", "spark", "unused", xs @ _*) =>
    MergeStrategy.discard
  case x => MergeStrategy.defaultMergeStrategy(x)
}

Compile / PB.targets := Seq(
  PB.gens.java -> (Compile / sourceManaged).value,
  scalapb.gen(javaConversions=true) -> (Compile / sourceManaged).value
)

lazy val root = (project in file("."))
  .settings(
    name := "mleap-serving",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += mleapRuntime,
    libraryDependencies += scalapbProtbuf,
    libraryDependencies += scalapbJson4s,
    libraryDependencies += springBoot,
    libraryDependencies += springBootActuator,
    libraryDependencies += springBootTest,
    libraryDependencies += protobufJavaUtil
  )
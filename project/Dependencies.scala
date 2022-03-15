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

import sbt._

object Dependencies {

  val springBootVersion = "2.6.2"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.8"
  lazy val mleapRuntime = "ml.combust.mleap" %% "mleap-runtime" % "0.19.0"
  lazy val scalapbJson4s = "com.thesamet.scalapb" %% "scalapb-json4s" % "0.7.2"
  lazy val scalapbProtbuf = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  lazy val springBoot = "org.springframework.boot" % "spring-boot-starter-web" % springBootVersion
  lazy val springBootActuator = "org.springframework.boot" % "spring-boot-starter-actuator" % springBootVersion
  lazy val springBootTest = "org.springframework.boot" % "spring-boot-starter-test" % springBootVersion % "test"
  lazy val protobufJavaUtil = "com.google.protobuf" % "protobuf-java-util" % "3.18.1"
}

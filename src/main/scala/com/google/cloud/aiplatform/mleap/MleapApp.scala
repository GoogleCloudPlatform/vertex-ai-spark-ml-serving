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

package com.google.cloud.aiplatform.mleap

import com.google.cloud.aiplatform.mleap.configuration.MleapConfig
import com.google.cloud.aiplatform.mleap.prediction.{
  MleapModel, PredictionSchema, ProtoMapper
}

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter
import org.springframework.web.client.RestTemplate
import scala.collection.JavaConverters._
import scala.io.Source
import scalapb.json4s.JsonFormat

@SpringBootApplication
class MleapApp @Autowired()(appConfig: MleapConfig) {

  /** Parses JSON to protos and vice-versa for requests and responses */
  @Bean
  def protobufJsonFormatHttpMessageConverter = new ProtobufJsonFormatHttpMessageConverter

  /** Model input schema used for prediction requests */
  @Bean
  def mleapSchema: ProtoMapper.MleapSchema = {
    ProtoMapper.PredictionSchemaMapper(sys.env.get("MLEAP_SCHEMA") match {
      case Some(schemaStr) =>
        JsonFormat.fromJsonString[PredictionSchema](schemaStr)
      case None =>
        JsonFormat.fromJsonString[PredictionSchema](
          Option(appConfig.modelSchemaPath) match {
            case Some(modelSchemaPath) =>
              Source.fromFile(modelSchemaPath).getLines.mkString
            case None =>
              throw new Exception("Unable to load schema from MLEAP_SCHEMA or from mleap.serving.modelSchemaPath in application.properties")
          })
    }).to[ProtoMapper.MleapSchema]
  }

  /** MLeap model used for prediction */
  @Bean
  def mleapModel: MleapModel = {
    sys.env.get("MLEAP_BUNDLE_PATH").orElse(Option(appConfig.modelBundlePath)) match {
      case Some(modelBundlePath) =>
        MleapModel(modelBundlePath, mleapSchema)
      case None =>
        throw new Exception(
          """Unable to load model from MLEAP_BUNDLE_PATH or from mleap.serving.modelBundlePath in application.properties""")
    }
  }
}

object Application extends App {
  SpringApplication.run(classOf[MleapApp]);
}

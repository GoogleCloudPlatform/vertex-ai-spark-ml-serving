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

package com.google.cloud.aiplatform.mleap.prediction

import ml.combust.bundle.BundleFile
import ml.combust.mleap.runtime.MleapSupport.MleapBundleFileOps
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, LeapFrame, Row}
import ml.combust.mleap.runtime.frame.Transformer
import ml.combust.mleap.runtime.serialization.FrameReader
import resource.managed
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalapb.json4s.Parser

/** A MLeap model that can be used for prediction
 */
class MleapModel(pipeline: Transformer, schema: ProtoMapper.MleapSchema) {

  /** Returns predictions from the model
   *
   *  @param frame An input DefaultLeapFrame with prediction instances
   *  @return A DefaultLeapFrame containing the prediction results
   */
  def predict(frame: DefaultLeapFrame): ProtoMapper.MleapPredictionResult = {
    pipeline.transform(frame) match {
      case Success(result) => ProtoMapper.MleapPredictionResult(result, schema.output)
      case Failure(f) => throw f
    }
  }
}

/** Factory for [[com.google.cloud.aiplatform.mleap.prediction.MleapModel]]
 *  instances.
 */
object MleapModel {

  /** Creates a MleapModel instances
   *
   *  @param modelJarPath Path to the model bundle file
   *  @param schema The model schema
   *  @return A MleapModel instance
   */
  def apply(modelJarPath: String, schema: ProtoMapper.MleapSchema): MleapModel = {

    (for(bundleFile <- managed(BundleFile("jar:file:" + modelJarPath)))
     yield bundleFile.loadMleapBundle().get)
     .tried match {
        case Success(bundle) =>
          val pipeline = bundle.root
          val mLeapModel = new MleapModel(pipeline, schema)
          mLeapModel
        case Failure(f) => throw f
      }
  }
}

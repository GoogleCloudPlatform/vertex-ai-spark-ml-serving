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
import ml.combust.bundle.serializer.SerializationFormat
import ml.combust.mleap.core.feature.VectorAssemblerModel
import ml.combust.mleap.core.types.{
  BasicType, ListType, NodeShape, ScalarShape, ScalarType, StructField, StructType, TensorType
}
import ml.combust.mleap.runtime.MleapSupport.MleapTransformerOps
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, Row}
import ml.combust.mleap.runtime.transformer.{Pipeline, PipelineModel}
import ml.combust.mleap.runtime.transformer.feature.VectorAssembler
import ml.combust.mleap.tensor.{DenseTensor, Tensor}
import org.scalatest.{flatspec, GivenWhenThen}
import resource.managed
import scalapb.json4s.JsonFormat

object MleapTestModel {

  def fixtures = new {
    val inputSchema = StructType(
      "myFloat1" -> ScalarType.Float,
      "myFloat2" -> ScalarType.Float,
      "myDouble1" -> ScalarType.Double,
      "myString1" -> ScalarType.String,
      "myInt1" -> ScalarType.Int,
      "myShort1" -> ScalarType.Short,
      "myLong1" -> ScalarType.Long,
      "myFloatList1" -> ListType(BasicType.Float),
      "myByteTensor1" -> TensorType(BasicType.Byte),
      "myDoubleTensor1" -> TensorType(BasicType.Double)).get
    val outputSchema = StructType(inputSchema.fields ++
       Seq(StructField("myDoubleTensor2", TensorType(BasicType.Double)))).get
  }

  def createTestModel(modelPath: String): MleapModel = {

    val f = fixtures

    val featureAssembler = VectorAssembler(
      shape = NodeShape().withInput("input0", "myFloat1")
        .withInput("input1", "myFloat2")
        .withStandardOutput("myDoubleTensor2"),
      model = VectorAssemblerModel(Seq(ScalarShape(), ScalarShape())))

    val pipeline: Pipeline = Pipeline(
      shape = NodeShape(),
      model = PipelineModel(Seq(featureAssembler)))

    for(bundle <- managed(BundleFile("jar:file:" + modelPath))) {
      pipeline.writeBundle.format(SerializationFormat.Json).save(bundle)
    }

    val mleapSchema = ProtoMapper.MleapSchema(f.inputSchema, f.outputSchema)
    MleapModel(modelPath, mleapSchema)
  }
}

class MleapModelSpec extends flatspec.AnyFlatSpec with GivenWhenThen {

  def withModel(testCode: MleapModel => Any): Unit = {
    val modelPath = "/tmp/simple-model.zip"
    val mleapModel = MleapTestModel.createTestModel(modelPath)
    testCode(mleapModel)
  }

  "An MleapModel" should "transform an input frame into an output frame" in withModel { model =>
    val f = MleapTestModel.fixtures

    Given("a DefaultLeapFrame")
    val rows = Seq(Row(
        1.0f,                                           // myFloat1
        2.0f,                                           // myFloat2
        3.0d,                                           // myDouble1
        "foo",                                          // myString1
        Int.MaxValue,                                   // myInt1
        Short.MaxValue,                                 // myShort1
        Long.MaxValue,                                  // myLong1
        Seq[Float](1.0f, 2.0f, 3.0f),                   // myFloatList1
        Tensor.denseVector(Array[Byte](1, 2, 3, 4)),    // myByteTensor1
        Tensor.denseVector(Array[Double](5.0f, 6.0f)))) // myDoubleTensor1

    val frame = DefaultLeapFrame(f.inputSchema, rows)

    When("the model transforms the input frame")
    val result = model.predict(frame)

    Then("the transformed output frame should match the expected output")
    val outputFrame = DefaultLeapFrame(
      f.inputSchema, Seq(Row(
        1.0f,                                          // myFloat1
        2.0f,                                          // myFloat2
        3.0d,                                          // myDouble1
        "foo",                                         // myString1
        Int.MaxValue,                                  // myInt1
        Short.MaxValue,                                // myShort1
        Long.MaxValue,                                 // myLong1
        Seq[Float](1.0f, 2.0f, 3.0f),                  // myFloatList1
        Tensor.denseVector(Array[Byte](1, 2, 3, 4)),   // myByteTensor1
        Tensor.denseVector(Array[Double](5.0f, 6.0f)), // myDoubleTensor1
        Tensor.denseVector(Array[Double](1.0d, 2.0d))) // myDoubleTesnsor2
      ))

    assert(result.frame.dataset.equals(outputFrame.dataset))
  }
}

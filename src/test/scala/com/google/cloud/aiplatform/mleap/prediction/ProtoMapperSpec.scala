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

import com.google.protobuf.struct.{Value, ListValue}
import ml.combust.mleap.core.types.{
  BasicType, ListType, ScalarType, StructField, StructType, TensorType
}
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, Row}
import ml.combust.mleap.tensor.Tensor
import org.scalatest.{flatspec, GivenWhenThen}
import scalapb.json4s.JsonFormat

class ProtoMapperSpec extends flatspec.AnyFlatSpec with GivenWhenThen {

  "A PredictionSchema" should "be mappable to a MleapSchema" in {
    Given("a PredictionSchema converted from JSON")
    val schemaStr = """
      {
        "input": [
          {
            "name": "myFloat1",
            "type": "FLOAT"
          },
          {
            "name": "myDouble1",
            "type": "DOUBLE"
          },
          {
            "name": "myString1",
            "type": "STRING"
          },
          {
            "name": "myInt1",
            "type": "INTEGER"
          },
          {
            "name": "myShort1",
            "type": "SHORT"
          },
          {
            "name": "myLong1",
            "type": "LONG"
          },
          {
            "name": "myDoubleArray1",
            "type": "DOUBLE",
            "struct": "ARRAY"
          },
          {
            "name": "myDoubleVector1",
            "type": "DOUBLE",
            "struct": "VECTOR"
          }
        ],
        "output": [
          {
            "name": "probability",
            "type": "DOUBLE",
            "struct": "VECTOR"
          }
        ]
      }
    """
    val schema = JsonFormat.fromJsonString[PredictionSchema](schemaStr)

    When("the mapper is called")
    val mleapSchema = ProtoMapper.PredictionSchemaMapper(schema).to[ProtoMapper.MleapSchema]

    Then("the MleapSchema should be valid")
    val input = StructType(
      StructField("myFloat1", ScalarType.Float),
      StructField("myDouble1", ScalarType.Double),
      StructField("myString1", ScalarType.String),
      StructField("myInt1", ScalarType.Int),
      StructField("myShort1", ScalarType.Short),
      StructField("myLong1", ScalarType.Long),
      StructField("myDoubleArray1", ListType(BasicType.Double)),
      StructField("myDoubleVector1", TensorType(BasicType.Double))).get
    val output = StructType(
      StructField("probability", TensorType(BasicType.Double))).get
    val baselineSchema = ProtoMapper.MleapSchema(input, output)

    assert(mleapSchema == baselineSchema)
  }

  "A PredictionRequest" should "be mappable to a DefaultLeapFrame" in {

    Given("a StructType schema")
    val schema: StructType = StructType(
      StructField("float1", ScalarType.Float),
      StructField("float2", ScalarType.Float),
      StructField("float3", ScalarType.Float),
      StructField("tensor1", ListType.String)
    ).get

    And("a PredictionRequest converted from a JSON")
    val requestStr = """
      {
        "instances": [
          [1.0, 2.0, 3.0, ["foo", "bar"]]
        ]
      }
    """
    val request = JsonFormat.fromJsonString[PredictionRequest](requestStr)

    When("the mapper is called")
    val requestFrame = ProtoMapper.PredictionRequestMapper(request, schema).to[DefaultLeapFrame]

    Then("the DefaultLeapFrame should match the expected output")
    val baselineFrame = DefaultLeapFrame(
      schema, Seq(Row(1.0, 2.0, 3.0, Seq("foo", "bar")))
    )

    assert(requestFrame == baselineFrame)
  }

  "An MleapPredictionResult" should "be mappable to a PredictionResponse" in {

    Given("A MleapPredictionResult object")

    val testSchema = StructType(
      "myFloat1" -> ScalarType.Float,
      "myFloat2" -> ScalarType.Float,
      "myDouble1" -> ScalarType.Double,
      "myString1" -> ScalarType.String,
      "myInt1" -> ScalarType.Int,
      "myShort1" -> ScalarType.Short,
      "myLong1" -> ScalarType.Long,
      "myFloatList1" -> ListType(BasicType.Float),
      "myDoubleTensor1" -> TensorType(BasicType.Double)).get

    val frame = DefaultLeapFrame(
      testSchema, Seq(Row(
        1.0f,                                          // myFloat1
        2.0f,                                          // myFloat2
        3.0d,                                          // myDouble1
        "foo",                                         // myString1
        Int.MaxValue,                                  // myInt1
        Short.MaxValue,                                // myShort1
        Long.MaxValue,                                 // myLong1
        Seq[Float](1.0f, 2.0f, 3.0f),                  // myFloatList1
        Tensor.denseVector(Array[Double](5.0d, 6.0d))  // myDoubleTensor1
      )))

    val predictionResult = ProtoMapper.MleapPredictionResult(frame, testSchema)

    When("the mapper is called")
    val response = predictionResult.as[PredictionResponse]

    Then("the PredictionResponse object should match the expected output")
    val baselineResponse = PredictionResponse(Seq(
      ListValue(Seq(Value(
        Value.Kind.NumberValue(1.0f)),
        Value(Value.Kind.NumberValue(2.0f)),
        Value(Value.Kind.NumberValue(3.0d)),
        Value(Value.Kind.StringValue("foo")),
        Value(Value.Kind.NumberValue(Int.MaxValue)),
        Value(Value.Kind.NumberValue(Short.MaxValue)),
        Value(Value.Kind.NumberValue(Long.MaxValue)),
        Value(Value.Kind.ListValue(ListValue(Seq(Value(
          Value.Kind.NumberValue(1.0f)),
          Value(Value.Kind.NumberValue(2.0f)),
          Value(Value.Kind.NumberValue(3.0f)))))),
        Value(Value.Kind.ListValue(ListValue(Seq(Value(
          Value.Kind.NumberValue(5.0d)),
          Value(Value.Kind.NumberValue(6.0d))))))))
    ))

    assert(response == baselineResponse)
  }
}

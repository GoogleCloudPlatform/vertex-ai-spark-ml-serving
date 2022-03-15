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

import com.google.protobuf.struct.{ListValue, Value}
import ml.combust.mleap.core.types.{
  BasicType, DataType, ListType, ScalarType, StructField, StructType, TensorType
}
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, LeapFrame, Row}
import ml.combust.mleap.tensor.Tensor
import org.apache.spark.ml.linalg.Vectors;

/** Helpers to convert between protobufs and API domain objects */
object ProtoMapper {

  /** Conversion helper from PredictionSchema => T */
  case class PredictionSchemaMapper(schema: PredictionSchema) {
    def to[T](implicit f: PredictionSchemaMapper => T): T = f(this)
  }

  /** Factory for
   *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.PredictionSchemaMapper]]
   *  instances.
   */
  object PredictionSchemaMapper {

    /** Creates a
     *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.PredictionSchemaMapper]]
     *  instance from a model input schema.
     *
     *  @param schemaProto A MLeap model schema proto
     *  @return A PredictionSchemaMapper initialized with the model input schema
     */
    def apply(schemaProto: PredictionSchema): PredictionSchemaMapper = {
      val schemaMapper = new PredictionSchemaMapper(schemaProto)
      schemaMapper
    }

    private def protoEnumToMleapType(schemaField: PredictionSchemaField): StructField = {
      val basicType = schemaField.`type` match {
        case PredictionSchemaField.FieldType.DOUBLE  => BasicType.Double
        case PredictionSchemaField.FieldType.FLOAT   => BasicType.Float
        case PredictionSchemaField.FieldType.STRING  => BasicType.String
        case PredictionSchemaField.FieldType.BOOLEAN => BasicType.Boolean
        case PredictionSchemaField.FieldType.BYTE    => BasicType.Byte
        case PredictionSchemaField.FieldType.INTEGER => BasicType.Int
        case PredictionSchemaField.FieldType.SHORT   => BasicType.Short
        case PredictionSchemaField.FieldType.LONG    => BasicType.Long
        case other =>
          throw new MleapSchemaException(
            s"""Unexpected type for field "${schemaField.name}": $other""")
      }
      val structure = schemaField.struct match {
        case PredictionSchemaField.FieldStruct.BASIC  => ScalarType(basicType)
        case PredictionSchemaField.FieldStruct.ARRAY  => ListType(basicType)
        case PredictionSchemaField.FieldStruct.VECTOR => TensorType(basicType)
        case other =>
          throw new MleapSchemaException(
            s"""Unexpected structure for field "${schemaField.name}": $other""")
      }
      StructField(schemaField.name, structure)
    }

    /** Implicit conversion from [[com.google.cloud.aiplatform.mleap.prediction.PredictionSchema]]
     *  to [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.MleapSchema]]
     *
     *  Example usage:
     *
     *  {{{
     *  val schema: PredictionSchema = ...
     *  PredictionSchemaMapper(schema).to[MleapSchema]
     *  }}}
     *
     *  @param schemaMapper A
     *   [[com.google.cloud.aiplatform.mleap.prediction.PredictionSchemaMapper]] instance
     *
     *  @return A [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.MleapSchema]]
     *   instance
     */
    implicit def schemaMapper = (schemaMapper: PredictionSchemaMapper) => {
      val inputFields = schemaMapper.schema.input.map(protoEnumToMleapType _)
      val outputFields = schemaMapper.schema.output.map(protoEnumToMleapType _)
      MleapSchema(
        StructType(inputFields.head, inputFields.tail: _*).get,
        StructType(outputFields.head, outputFields.tail: _*).get
      )
    }
  }

  /** Implicit conversion helper from PredictionRequest => T */
  case class PredictionRequestMapper(request: PredictionRequest, schema: StructType) {
    def to[T](implicit f: PredictionRequestMapper => T): T = f(this)
  }

  /** Factory for
   *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.PredictionRequestMapper]]
   *  instances.
   */
  object PredictionRequestMapper {

    /** Creates a
     *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.PredictionRequestMapper]]
     *  instance from a prediction request and a MLeap schema.
     *
     *  @param requestProto A prediction request proto
     *  @return A PredictionSchemaMapper initialized with the model input schema
     */
    def apply(requestProto: PredictionRequest, schema: StructType): PredictionRequestMapper = {
      val requestMapper = new PredictionRequestMapper(requestProto, schema)
      requestMapper
    }

    /** Converts a [[com.google.cloud.aiplataform.mleap.prediction.PredictionRequest]]
     *  to a [[ml.combust.mleap.runtime.frame.DefaultLeapFrame]]
     *
     *  Example usage:
     *
     *  {{{
     *  val request: PredictionRequest = ...
     *  PredictionRequestMapper(request).to[DefaultLeapFrame]
     *  }}}
     *
     *  @param requestMapper A
     *   [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.PredictionRequestMapper]]
     *   instance
     *
     *  @return A [[ml.combust.mleap.runtime.frame.DefaultLeapFrame]] instance
     */
    implicit def requestMapper = (requestMapper: PredictionRequestMapper) => {
      val rows = requestMapper.request.instances map { x =>
        if (x.values.length != requestMapper.schema.fields.length)
          throw new MleapSchemaException("Mismatched number of values and schema fields.")
        Row((x.values zip requestMapper.schema.fields) map {
          case (inputVal, schemaField) => protoValueToMleapValue(inputVal, schemaField)
        }: _*)
      }
      DefaultLeapFrame(requestMapper.schema, rows)
    }

    /** Converts a protobuf ListValue to a Scala type that can be added to a MLeap Row */
    private def protoListToRowValue(listVal: ListValue, schemaField: StructField): Any = {
      schemaField.dataType match {
        case _: TensorType =>
          if (schemaField.dataType.base == BasicType.Double)
            Vectors.dense(Array[Double](listVal.values.map {
              protoValueToMleapValue(_, schemaField).asInstanceOf[Double]
            }: _*))
          else throw new MleapSchemaException(
            s"""Cannot assign non-Double TensorType field "${schemaField.name}".""")
        case _: ListType => listVal.values.map(protoValueToMleapValue(_, schemaField))
        case _: ScalarType =>
          throw new MleapSchemaException(
            s"""Cannot assign Array to ScalarType field "${schemaField.name}.""")
        case _ =>
          throw new MleapSchemaException(
            s"""Cannot determine type of field "${schemaField.name} to assign value".""")
      }
    }

    /** Converts a protobuf Value to a Scala type that can be added to a MLeap Row */
    private def protoValueToMleapValue(inputVal: Value, schemaField: StructField): Any = {
      (inputVal.kind, schemaField.dataType.base) match {
        case (Value.Kind.StringValue(_), BasicType.String) => inputVal.getStringValue
        case (Value.Kind.NumberValue(_), BasicType.Int)    => inputVal.getNumberValue.toInt
        case (Value.Kind.NumberValue(_), BasicType.Float)  => inputVal.getNumberValue.toFloat
        case (Value.Kind.NumberValue(_), BasicType.Double) => inputVal.getNumberValue.toDouble
        case (Value.Kind.NumberValue(_), BasicType.Long)   => inputVal.getNumberValue.toLong
        case (Value.Kind.NumberValue(_), BasicType.Short)  => inputVal.getNumberValue.toShort
        case (Value.Kind.StringValue(_), BasicType.Byte)   => inputVal.getStringValue.toByte
        case (Value.Kind.BoolValue(_), BasicType.Boolean)  => inputVal.getBoolValue
        case (Value.Kind.ListValue(lv), _)                 => protoListToRowValue(lv, schemaField)
        case _ =>
          throw new MleapSchemaException(
            s"""Cannot assign value ${inputVal.toProtoString} to field "${schemaField.name}".""")
      }
    }
  }

  /** Stores the model input and output schema */
  case class MleapSchema(input: StructType, output: StructType)

  /** Conversion helper from DefaultLeapFrame => T */
  case class MleapPredictionResult(frame: DefaultLeapFrame, schema: StructType) {

    private val columnIdxMap = Map((for {
        (i, idx) <- frame.schema.fields.zipWithIndex
        j <- schema.fields
        if i.name == j.name
      } yield (i.name, idx)): _*)

    def as[T](implicit f: MleapPredictionResult => T): T = f(this)

    def show = frame.show()
  }

   /** Factory for
    *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.MleapPredictionResult]]
    *  instances.
    */
  object MleapPredictionResult {
    def apply(frame: DefaultLeapFrame, schema: StructType): MleapPredictionResult = {
      val result = new MleapPredictionResult(frame, schema)
      result
    }

    private def mleapValueToProtoListValue(dataType: DataType, seqVal: Seq[Any]): Value = {
      dataType match {
        case ListType(base, _) =>
          Value(Value.Kind.ListValue(ListValue(
            seqVal.map(x => mleapValueToProtoValue(dataType, x)))))
        case _ =>
          throw new MleapSchemaException("Sequence returned for non ListType.")
      }
    }

    /** Converts a MLeap Row value to a protobuf Value */
    private def mleapValueToProtoValue(dataType: DataType, value: Any): Value = {
      (dataType.base, value) match {
        case (BasicType.String, strVal: String)       => Value(Value.Kind.StringValue(strVal))
        case (BasicType.Int, intVal: Int)             => Value(Value.Kind.NumberValue(intVal))
        case (BasicType.Float, floatVal: Float)       => Value(Value.Kind.NumberValue(floatVal))
        case (BasicType.Double, doubleVal: Double)    => Value(Value.Kind.NumberValue(doubleVal))
        case (BasicType.Long, longVal: Long)          => Value(Value.Kind.NumberValue(longVal))
        case (BasicType.Short, shortVal: Short)       => Value(Value.Kind.NumberValue(shortVal))
        case (BasicType.Boolean, boolVal: Boolean)    => Value(Value.Kind.BoolValue(boolVal))
        case (BasicType.Double, tensorVal: Tensor[_]) =>
          Value(Value.Kind.ListValue(ListValue(
            tensorVal.toArray.map(x => mleapValueToProtoValue(dataType, x)))))
        case (_, seqVal: Seq[Any]) => mleapValueToProtoListValue(dataType, seqVal)
        case other =>
          throw new MleapSchemaException(s"Unable to determine value for Mleap basic type: $other")
      }
    }

    /** Implicit conversion from
     *  [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.MleapPredictionResult]]
     *  to [[com.google.cloud.aiplatform.mleap.prediction.PredictionResponse]]
     *
     *  Example usage:
     *
     *  {{{
     *  val mleapPredictionResult: MleapPredictionResult = ...
     *  MleapPredictionResult.as[PredictionResponse]
     *  }}}
     *
     *  @return A [[com.google.cloud.aiplatform.mleap.prediction.PredictionResponse]]
     *   instance
     */
    implicit def resultMapper = (result: MleapPredictionResult) =>
      PredictionResponse().withPredictions(
        result.frame.dataset map { x =>
          ListValue(result.schema.fields map { y =>
            mleapValueToProtoValue(y.dataType, x.get(result.columnIdxMap(y.name)))
          })
        })
  }

  /** Exception class for MLeap schema errors
   */
  class MleapSchemaException(message: String, cause: Throwable = null)
    extends Exception(message, cause)

  /** Factory for [[com.google.cloud.aiplatform.mleap.prediction.ProtoMapper.MleapSchemaException]]
   *  instances.
   */
  object MleapSchemaException {
    def apply(message: String): MleapSchemaException = new MleapSchemaException(message)
    def apply(message: String, cause: Throwable): MleapSchemaException =
      new MleapSchemaException(message, cause)
  }
}

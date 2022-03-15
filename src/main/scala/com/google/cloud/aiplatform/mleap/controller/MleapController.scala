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

import com.google.cloud.aiplatform.mleap.prediction.{PredictionRequest, PredictionResponse, ProtoMapper}
import com.google.cloud.aiplatform.mleap.service.MleapService
import ml.combust.mleap.runtime.frame.DefaultLeapFrame
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter
import org.springframework.web.bind.annotation.{
  RequestBody, RequestMapping, RequestMethod, ResponseBody, RestController
}
import org.springframework.web.server.ResponseStatusException
import scala.util.{Failure, Success, Try}

/** Controller for serving prediction and health-check requests */
@RestController
class MleapController @Autowired()(mleapService: MleapService) {

  @RequestMapping(value = Array("/predict"), method = Array(RequestMethod.POST),
    produces = Array(MediaType.APPLICATION_JSON_VALUE))
  @ResponseBody
  def processRequest(@RequestBody request: Prediction.PredictionRequest): Prediction.PredictionResponse = {
    Try {
      mleapService.getPrediction(PredictionRequest.fromJavaProto(request))
    }
    match {
      case Success(r) => PredictionResponse.toJavaProto(r)
      case Failure(e: ProtoMapper.MleapSchemaException) =>
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e)
      case Failure(e) =>
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e)
    }
  }

  @RequestMapping(value = Array("/health"), method = Array(RequestMethod.GET))
  def healthCheck(): String = {
    "OK"
  }
}

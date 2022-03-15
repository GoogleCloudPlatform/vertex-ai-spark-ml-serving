# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Example prediction client for the Spark ML serving tutorial."""

import argparse
import asyncio
import random

from google.api_core.client_options import ClientOptions
from google.cloud import aiplatform_v1
from google.cloud.aiplatform_v1.types import PredictRequest
from google.protobuf import struct_pb2


def parse_arguments():
  """Parses command-line arguments."""
  parser = argparse.ArgumentParser()
  parser.add_argument('--project', type=str, required=True,
                      help='Your project ID.')
  parser.add_argument('--endpoint', type=str, required=True,
                      help='Your endpoint ID.')
  parser.add_argument('--location', type=str, required=True,
                      help='Your endpoint\'s location')
  parser.add_argument('--num_instances', type=int, default=3,
                      help='Number of instances per request.')
  parser.add_argument('--num_requests', type=int, default=10,
                      help='Number of instances per request.')
  args = parser.parse_args()
  return args


def print_result(request_id, instances, predictions):
  """Helper to print an instance and its corresponding prediction."""
  print(f'\n==> Response from request #{request_id}:\n')
  for i in range(len(instances)):
    print(f'Instance {i+1}:\tsepal_length:\t{instances[i][0].number_value}\n'
          f'\t\tsepal_width:\t{instances[i][1].number_value}\n'
          f'\t\tpetal_length:\t{instances[i][2].number_value}\n'
          f'\t\tpetal_width:\t{instances[i][3].number_value}\n')
    print(f'Prediction {i+1}:\tsetosa:\t\t{predictions[i][0][0]}\n'
          f'\t\tversicolor:\t{predictions[i][0][1]}\n'
          f'\t\tvirginica:\t{predictions[i][0][2]}\n')


def generate_single_instance():
  """Generate a single prediction instance using randomly-generated values."""
  instance = [
      struct_pb2.Value(number_value=random.uniform(5.0, 8.0)),
      struct_pb2.Value(number_value=random.uniform(2.0, 5.0)),
      struct_pb2.Value(number_value=random.uniform(1.0, 7.0)),
      struct_pb2.Value(number_value=random.uniform(0.1, 3.0))
    ]
  return instance


async def predict(request_id, client, endpoint, num_instances):
  """Sends a prediction request and prints the response."""
  instances = [generate_single_instance() for _ in range(num_instances)]
  request = PredictRequest(endpoint=endpoint)
  request.instances.extend(instances)
  response = await client.predict(request=request)
  print_result(request_id, instances, response.predictions)


async def main():
  """Main function."""
  args = parse_arguments()
  api_endpoint = f'{args.location}-aiplatform.googleapis.com'
  client_options = ClientOptions(api_endpoint=api_endpoint)
  client = aiplatform_v1.PredictionServiceAsyncClient(
      client_options=client_options)
  endpoint = f'projects/{args.project}/locations/{args.location}/endpoints/{args.endpoint}'
  print(f'Sending {args.num_requests} asynchronous prediction requests with',
        f'{args.num_instances} instances per request ...')
  await asyncio.gather(*[predict(i+1, client, endpoint, args.num_instances)
                         for i in range(args.num_requests)])


if __name__ == '__main__':
  asyncio.run(main())

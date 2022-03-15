#!/bin/bash

# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [[ ! -z ${AIP_STORAGE_URI} ]]; then
    # Copy Mleap bundle from Cloud Storage
    mkdir /tmp/model && gsutil cp -r ${AIP_STORAGE_URI} /tmp/model 2>&1
    [[ "$?" -ne 0 ]] && echo "gsutil copy command failed. Inspect the copy command output for details." >&2 && exit 1
    # Move Mleap bundle to the location configured in application.properties
    find /tmp/model -name "model.zip" -exec mv -v {} /app \;
    [[ ! -f /app/model.zip ]] && echo "Model file at /app/model.zip does not exist. Does model.zip exist at ${AIP_STORAGE_URI}? Exiting." >&2 && exit 1
else
    echo "Env variable AIP_STORAGE_URI is not set or is empty. Usually this means you didn't set artifactUri in Vertex when deploying the model. Exiting." >&2
    exit 1
fi

if [[ -z ${MLEAP_SCHEMA} ]]; then
    # Move the schema file to the location configured in application.properties
    find /tmp/model -name "schema.json" -exec mv -v {} /app \;
    [[ ! -f /app/schema.json ]] && echo "Env variable MLEAP_SCHEMA is not set or is empty, and /app/schema.json does ot exist. Does schema.json exist at ${AIP_STORAGE_URI}? Exiting." >&2 && exit 1
fi

# Start the server
exec java -jar "bin/mleap-serving-assembly-0.1.0-SNAPSHOT.jar"
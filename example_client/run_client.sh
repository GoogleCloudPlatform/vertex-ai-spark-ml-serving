#!/usr/bin/env sh

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

BASE_DIR=$(dirname "$0")
VENV_DIR="${BASE_DIR}/venv"

if [ ! -d "${VENV_DIR}" ]; then
    echo "Setting up python virtual environment ..."
    python3 -m venv ${VENV_DIR} && source ${VENV_DIR}/bin/activate &&
    pip3 install -r ${BASE_DIR}/requirements.txt &&
    deactivate
fi

source ${VENV_DIR}/bin/activate && python3 ${BASE_DIR}/main.py $@ && deactivate

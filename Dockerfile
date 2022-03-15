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

FROM openjdk:11-jre-slim-buster

RUN apt-get update && \
    apt-get install -y curl gnupg2 ca-certificates lsb-release

RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" \
    | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg \
    | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add - && apt-get update -y && apt-get install google-cloud-sdk -y

RUN apt-get remove -y curl gnupg2
RUN rm -rf /var/lib/apt/lists/*

ENV APP_HOME /app
ENV APP_JAR target/scala-2.12/mleap-serving-assembly-0.1.0-SNAPSHOT.jar

RUN groupadd -r app && useradd -r -gapp app
RUN mkdir -m 0755 -p ${APP_HOME}/bin

COPY ${APP_JAR} ${APP_HOME}/bin
COPY entrypoint.sh /

RUN chmod +x /entrypoint.sh
RUN chown -R app:app ${APP_HOME}

EXPOSE 8080

WORKDIR ${APP_HOME}

CMD ["/entrypoint.sh"]
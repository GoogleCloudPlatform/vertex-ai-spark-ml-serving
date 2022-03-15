# Serving Spark ML models using Vertex AI

This repository contains the companion code for [Serving Spark ML models using Vertex AI](https://cloud.google.com/architecture/spark-ml-model-with-vertexai). The code shows you how to serve (run) [online predictions](https://cloud.devsite.corp.google.com/vertex-ai/docs/predictions/online-predictions-custom-models) from Spark MLlib models using Vertex AI.

The code allows you to build a [custom container for serving predictions](https://cloud.devsite.corp.google.com/vertex-ai/docs/predictions/use-custom-container) that can be used with Vertex AI. The custom container uses [MLeap](https://combust.github.io/mleap-docs/) to serve a Spark MLlib model that has been exported to an [MLeap Bundle](https://github.com/combust/mleap-docs/blob/master/core-concepts/mleap-bundles.md) (the MLeap serialization format). The MLeap execution engine and serialization format supports low-latency inference without dependencies on Spark. 

See the [MLeap documentation](https://combust.github.io/mleap-docs/spark/) for information on exporting Spark MLlib models to MLeap Bundles.

# How to use this example
Use the [tutorial](https://cloud.google.com/architecture/spark-ml-model-with-vertexai) to understand how to:

1. [Serve predictions from an example model](https://cloud.devsite.corp.google.com/architecture/spark-ml-model-with-vertexai#implementation) that is included with the tutorial. The example model has been trained using the [Iris dataset](https://archive.ics.uci.edu/ml/datasets/Iris), and then exported from Spark MLlib to an MLeap Bundle.

2. Configure the custom container image to [serve predictions from your own models](https://cloud.google.com/architecture/spark-ml-model-with-vertexai#serve_your_own_models).

# License
[Apache Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
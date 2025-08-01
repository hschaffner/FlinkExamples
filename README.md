# Confluent Platform Flink DataStream Examples with Confluent Schema Registry Records #

## Introduction ##
There are many Internet examples of Java-based Flink DataStream applications that utilize the 
Flink Kafka connector with Avro and JSON message payloads. However, those examples may be confusing due to the
multiple different versions of Flink. Many of the classes and methods for the serialization/deserialization
have been deprecated. As a result, many of the samples do not work when using
current versions of Flink. All the samples provided here are based on Flink 1.19. Equivalent version of the samples using version 1.2x will be added soon.

In the Confluent world, the problem with the Internet examples is that they do not include examples JSON and/or Avro 
that support a Schema Registry as part of their Kafka messaging. Generally, the registries include additional
information in the key and value for the Kafka records. This means that the Flink DataStream
applications cannot properly deserialize messages produced by non-Flink Kafka applications that include registry integration
when producing Kafka Records. As a result, these Kafka producer records that incorporate a registry will cause the Flink consumer to fail 
deserialization due to the additional information that is included in the Kafka record's key and value. 

The same issue is also relevant when Flink produces Kafka records. By default, Flink does not
natively understand how to serialize a Kafka record that requires schema registry details for the non-Flink Kafka consumers. Therefore, without adding the support to Flink
to (de)serialize Kafka records what support a schema registry, the benefits of using a schema registry are lost.

Confluent provides a supported version of Flink DataStream that operates in a Kubernetes environment. Confluent also 
provides the required Flink Operator to simplify the management and deployment of the Flink Jobs and Tasks. Confluent also supports their
own schema registry for Avro, JSON and Google Protobufs (however, these examples only include JSON and Avro.)

More details on Confluent's supported Flink DataStream product can be found here:

https://docs.confluent.io/platform/current/flink/overview.html

So how can Flink DataStreams applications interoperate with Confluent's Schema Registry (SR)? Well, it requires making use of the Confluent serializers that support
SR within the Flink serializers. There is a slightly different solution for Avro versus JSON. Also, most of the samples and class that support Flink's Kafka Connector focus mostly on the 
Kafka record value. We are including a sample that shows processing of Kafka Records where there is a different schema for the Kafka record key and value; with both integrated with SR.

## Included Samples Projects #

### *GenerateData* ###

To make is easier to run the included Flink samples, an application is used to generate 
messages that make use of Confluent's SR. It is a simple application created with Spring Cloud Streams and
the Spring Kafka binder. This application accepts REST JSON messages. The data generator then converts the JSON message to an Avro equivalent and adds a Java String as the key. The Avro message is written to the 
"flinkAvroTopic" topic. The data generator then consumes the newly created topic message and converts the Avro message to the JSON equivalent and creates an Avro key based on details in the original message. This newly created JSON/Avro messages is then 
written to the "flinkJsonTopic" topic.

A simple Curl command is used to send the REST message to the Data Generator application. An example of the JSON message and the Curl command can be found
in the "postTest.sh" file in the "GenerateData" project directory. It is important to note that you can keep sending the same message over and over and still get unique
messages generated in Kafka since the REST controller increments the CustomerID field before it is written to Kafka. Therefore, you can send multiple messages with simple scripts, for example:

```
#!/bin/zsh
i="0"
while [ $i -le 10 ] 
do
echo $i
curl -X POST -H "Content-Type: application/json" -d @data.json http://localhost:9090/test
i=$[$i+1]
done
```

### *SimpleConfluentFlinkSR1_19* ###

As the name implies, this is a simple Flink-Kafka application that provides both a 
Flink Source and a Flink Sink. This application consumes Kafka Records from the "flinkAvroTopic" topic and 
deserializes the Avro Kafka record value, sends it to standard output and then writes the same record, but without
a key, to the "flinkAvroTopicSink" topic.

The Flink consumer task deserializes Kafka record values written using schema registry by the Data Generator
application. The Flink Sink writes the Kafka records value as Avro messages, that also make use of the Confluent
SR serializer. The serialization/deserialization support for SR is easy to implement since the Confluent
serializer class, which supports SR, is part of the Apache Flink Java API. To further simplify the application, the
(de)serialization is directly against a POJO that Maven created directly from the schemas that are part of the project. 

It is important to note that the output that is written to standard output is not found in the Flink Task logs found in the Flink Job GUI. The
output is only visible in the log of from the Kubernetes Flink Task pod. 

### *SimpleConfluentFlinkSR1_19_JSON* ###

This Flink application provides a Flink consumer that reads Kafka values that were
created by the Data Generator from the "flinkJsonTopic" topic. These messages must support deserialization using the
Confluent JSON SR deserializer. Unlike with Avro, there is nothing in the Flink API to provide
the SR deserialization of the JSON record value. 

Fortunately, Flink allows the expansion of the Flink deserializer to support custom
deserializers. In this example, a "jsonSchemaDeserializationSchema" class was created to act
as the Flink deserializer for SR JSON messages. The custom deserializer is created by implementing 
the Flink "KafkaRecordDeserializationSchema<JsonMsg>" interface. The Confluent deserializer for JSON SR message
is configured and wrapped by the Flink interface via the "deserialize" method defined in the interface. 
The deserialized POJO (that was generated by Maven from the JSON schema) is then passed to the Flink
collector and then the data is written to stdio. As with the previous Flink application, the
output is only visible in the logs of the Flink Kubernetes application task pod. 

### *SimpleConfluentFlinkAvroSR1_19_AVRO_JSON* ###

This Flink application provides a Flink consumer that reads Kafka values that were written to the
"flinkJsonTopic" topic by the Data Generator application. These records have an Avro key and a JSON value.
This is closer to what clients would be using in production. Therefore, it is necessary to deserialize more
that just the Kafka record value as the previous two examples.

In this sample, a new deserializer class was created called "JsonAvroDeserializationSchema". This class
implements the "KafkaRecordDeserializationSchema<Tuple2<avroMsgK, JsonMsg>>" interface. However, unlike with the previous example, the definition of the return is
now a tuple of the POJOs for both the Avro and JSON generated from the schemas. The Flink deserializer
must now wrap both the Confluent Avro and JSON SR deserializers. This is achieved in the two additional classes that 
are added to the project to wrap both the required Confluent SR deserializers. The Flink collector now
expects a tuple of two POJOs rather than a single POJO. As with the previous Flink application, the
output is only visible in the logs of the Flink Kubernetes application task pod.

# Deploy and Run the Examples #

## Initial Setup Quick Start ##

First, it is expected that you have a running Kubernetes environment available. Second, it is
required that you have access to a public Docker repository where you have "read/write" permissions.

It is also expected that you have installed Skaffold on your development host. (Details about
Skaffold can be found here: https://skaffold.dev/). Although Skaffold is not mandatory, it greatly
simplifies development and testing. Without Skaffold you can still manually execute the steps required to 
compile the project, generate and store the Docker image and execute the Kubernetes command to rate the 
Flink Job and Task pods. 

It is important to note that the use of the Confluent FLink CLI is not used and the 
resulting YAML configuration file is no not compatible. If you want to execute the YAML file
using the CLI, you must:
- remove the reference to the environment
- change the API reference
- change the kind to "FlinkDeployment"

The required changes are in the example YAML configurations but are commented out.

The project was created against a simple "Confluent for Kubernetes Environment." This means that you must first install the Confluent Operator. It also
requires the creation of a namespace called "Confluent" and that it should be set as the default Kubernetes names space (as described in the CfK Quick Start Guide). While
you are installing Kubernetes operators, you may as well immediately install the two Confluent
Flink operators as described in the Confluent Platform Flink documentation. 
The sample
CfK YAML file that was used can be found in the "GenerateKafka" directory. From the directory a simple:

``` kubectl apply -f CfK_7.9.0_dataContract.yaml ```  

will create the entire Confluent Environment automatically. It is now possible to 
create all the topics and load the schemas against the topic with the simple command:

``` kubectl apply -f GenerateTopics ```

All YAML files in the "GenerateTopics" directory will create the topics and register the relevant required schemas. 

It also necessary to create the Flink environments to run the Job/Tasks. For these examples that can be done
with the command:

``` kubectl apply -f GenerateEnv ```

If you want to undo all the configurations, simply rerun the kubectl commands and replace
"apply" with "delete".

Finally, you want to generate the POJOs that are used with the serialization and deserialization. The
POJOs are created directly from the schemas in the projects and can be generated from a terminal session by changing 
to the selected example's directory and executing:

``` mvn generate-sources ```

(The Maven commands can also be called from the Intellij IDE.)

If you do a clean using Maven, you may need to again generate the POJOs with the command above.

Next, it is necessary to make sure you have installed Skaffold. Skaffold will completely
automate the deployment of the different projects. You can get more details about installing Skaffold on their website found here:

https://skaffold.dev/

After you install Skaffold, check that the ~/.skaffold/config file has been created and contains details
similar to:

```aiexclude
global:
  survey:
    last-taken: "2025-07-07T07:38:51-04:00"
    last-prompted: "2025-07-06T20:01:10-04:00"
  collect-metrics: true
  update:
    last-prompted: "2025-07-29T10:28:03-04:00"
kubeContexts:
  - kube-context: gke_csid-281116_us-east4-a_heinz-csid-281116-15442
    default-repo: docker.io/heinz57
```

Clearly, your config file should reflect your Kubernetes context and the default
repo for your Docker images. 

In each example's root directory you will find a "skaffold.yaml" file and a "skaffold.env" file. The "env" file is the tag that will be
appended to the docker image when it is placed in the Docker repo. It is important to make sure you also change that tag value in the "kube-manifests/flinktest.yaml" file's image field
or skaffold will load an old image rather than the most recently created docker image. 

Why are we using Skaffold for these examples? Skaffold greatly simplifies the deployment of applications 
in a Kubernetes environment. If you have the environment setup correctly and you have made changes to the code the simple command:

```skaffold dev```

will automatically:
- execute the equivalent of "mvn clean package"
- generate a Docker image from the produced JAR file
- push the Docker image to your chosen Docker Repository
- execute the equivalent of the Kubernetes "kubectl apply -f " against the rquired YAML files to launch the Flink job into Kubernetes

All output from the Kubernetes Pod will be sent to standard output. Executing a
control-C will automatically interrupt Skaffold and stop running Flink PODs and services. There are a lot more
options and features available with Skaffold; however, that is up to the reader to discover if they want to go
beyond simply running the examples in a developer mode.

If you want to force regeneration of the Docker image, simply do a "mvn clean package" and then run "skaffold dev" again.

Each of the examples is a Intellij project, and all projects are under a single workspace. If
you restart Intellij after you open the workspace the first time, you should notice that the "run/debug" window has changed.
At the very top of the software you will now have a "Develop on Kubernetes" option created when it restarts 
since it now sees there is a Skaffold configuration file. If you click the drop down
there should be configuration editing choice. In the configuration you need to add the Docker Repository that you are using, and make sure
the project is using the desired JVM version (current projects were tested with Java 17). You now can execute Skaffold directly from 
Intellij.

However, you can still manually do all the steps listed above to create and deploy the Flink
DataStream applications. If you already have a working image in Docker, you simply need to execute the YAML
file the example's "kube-manifests" directory without needing to use Skaffold, but using Skaffold will see the 
cached values have not required updating and will also automatically execute the YAML against the "kubectl" command.

### Running the Examples ###

You must first make sure that all the Setup and Quickstart instructions have been successfully executed. 

The first task is to deploy the Data Generator, by executing the "skaffold dev" command from the
command from the DataGenerator directory. You must then do Kubernetes port forwarding for the POD to allow access for the Curl
command from "localhost". Once this is up and running, you can again use Skaffold to start any 
one of the Flink examples. 

You can access to the Flink GUI by doing port forwarding for the Flink Job Pod.

Send a couple Curl commands and you should see the results in the Flink Task's pod logs.

To make it easy to deal with the Kubernetes environment, I highly recommend the K9S utility. It is a
very intuitive terminal window GUI that makes it easy to get the log details, port forwarding, etc. It can
be downloaded from:

https://k9scli.io/









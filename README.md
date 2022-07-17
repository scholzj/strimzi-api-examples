# Strimzi API Examples

This repository contains some simple examples of how to use the Strimzi `api` module.
The `api` module is available in [Maven repsoitories](https://mvnrepository.com/artifact/io.strimzi/api), so it can be easiy integrated into your Java applications. 
It can be used together with the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client) to manage Strimzi resources in your Kubernetes cluster.

* `KafkaExample`: Example which creates, reconfigures and deletes a Kafka cluster
* `PauseConnector`: Example which pauses an existing Kafka Connect connector


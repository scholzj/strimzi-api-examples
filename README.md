# Strimzi API Examples

This repository contains some simple examples of how to use the Strimzi `api` module.
The examples are currently based on the Strimzi 0.33.2 release.
The `api` module is available in [Maven repositories](https://mvnrepository.com/artifact/io.strimzi/api), so it can be easily integrated into your Java applications. 
It can be used together with the [Fabric8 Kubernetes Client](https://github.com/fabric8io/kubernetes-client) to manage Strimzi resources in your Kubernetes cluster.

## Installation examples

* `Install`: Installs the Strimzi Cluster Operator
* `Uninstall`: Uninstalls the Strimzi Cluster Operator

## Kafka examples

* `CreateKafka`: Deploys Kafka cluster
* `UpdateKafka`: Updates the Kafka cluster and waits for the rolling update to complete
* `DeleteKafka`: Deletes the Kafka cluster

## Connect examples

* `CreateConnectAndConnector`: Deploys Kafka Connect, Kafka topic and two connectors 
* `DeleteConnectAndConnector`: Delete Kafka Connect, Kafka topic and two connectors
* `PauseConnector`: Example which pauses an existing Kafka Connect connector
* `UnpauseConnector`: Example which pauses an existing Kafka Connect connector

## Cruise Control examples

* `Rebalance`: Triggers and approves cluster rebalance using Cruise Control and the `KafkaRebalance` custom resource. Once the rebalance is complete, it deletes it. 

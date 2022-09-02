package cz.scholz.strimzi.api.examples.connect;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteConnectAndConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteConnectAndConnector.class);
    private static final String NAMESPACE = "myproject";
    private static final String CONNECT_NAME = "my-connect";
    private static final String TOPIC_NAME = "my-topic";
    private static final String ECHO_CONNECTOR_NAME = "echo-connector";
    private static final String TIMER_CONNECTOR_NAME = "timer-connector";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Creating the Timer connector");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(TIMER_CONNECTOR_NAME).delete();

            LOGGER.info("Creating the Echo connector");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(ECHO_CONNECTOR_NAME).delete();

            LOGGER.info("Deleting the topic");
            Crds.topicOperation(client).inNamespace(NAMESPACE).withName(TOPIC_NAME).delete();

            LOGGER.info("Deleting the Kafka Connect cluster");
            Crds.kafkaConnectOperation(client).inNamespace(NAMESPACE).withName(CONNECT_NAME).delete();
        }
    }
}
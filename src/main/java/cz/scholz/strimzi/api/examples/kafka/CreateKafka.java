package cz.scholz.strimzi.api.examples.kafka;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CreateKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateKafka.class);
    private static final String NAMESPACE = "myproject";
    private static final String NAME = "my-cluster";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Checking if namespace {} exists", NAMESPACE);
            if (client.namespaces().withName(NAMESPACE).get() == null) {
                LOGGER.info("Creating namespace {}", NAMESPACE);
                Namespace ns = new NamespaceBuilder().withNewMetadata().withName(NAMESPACE).endMetadata().build();
                client.namespaces().resource(ns).create();
            }

            Kafka kafka = new KafkaBuilder()
                    .withNewMetadata()
                    .withName(NAME)
                        .withNamespace(NAMESPACE)
                    .endMetadata()
                    .withNewSpec()
                        .withNewZookeeper()
                            .withReplicas(3)
                            .withNewEphemeralStorage()
                            .endEphemeralStorage()
                        .endZookeeper()
                        .withNewKafka()
                            .withReplicas(3)
                            .withListeners(new GenericKafkaListenerBuilder()
                                    .withName("plain")
                                    .withType(KafkaListenerType.INTERNAL)
                                    .withPort(9092)
                                    .withTls(false)
                                    .build())
                            .withNewEphemeralStorage()
                            .endEphemeralStorage()
                        .endKafka()
                        .withNewEntityOperator()
                            .withNewTopicOperator()
                            .endTopicOperator()
                            .withNewUserOperator()
                            .endUserOperator()
                        .endEntityOperator()
                        .withNewCruiseControl()
                        .endCruiseControl()
                    .endSpec()
                    .build();

            LOGGER.info("Creating the Kafka cluster");
            Crds.kafkaOperation(client).inNamespace(NAMESPACE).resource(kafka).create();

            LOGGER.info("Waiting for the cluster to be ready");
            Crds.kafkaOperation(client).inNamespace(NAMESPACE).withName(NAME).waitUntilCondition(Kafka.isReady(), 5, TimeUnit.MINUTES);

            LOGGER.info("Kafka cluster is ready");
        }
    }
}
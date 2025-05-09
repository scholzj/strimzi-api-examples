package cz.scholz.strimzi.api.examples.kafka;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreateKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateKafka.class);
    private static final String NAMESPACE = "myproject";
    private static final String NAME = "my-cluster";
    private static final String POOL_NAME = "mixed";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Checking if namespace {} exists", NAMESPACE);
            if (client.namespaces().withName(NAMESPACE).get() == null) {
                LOGGER.info("Creating namespace {}", NAMESPACE);
                Namespace ns = new NamespaceBuilder().withNewMetadata().withName(NAMESPACE).endMetadata().build();
                client.namespaces().resource(ns).create();
            }

            KafkaNodePool pool = new KafkaNodePoolBuilder()
                    .withNewMetadata()
                        .withName(POOL_NAME)
                        .withNamespace(NAMESPACE)
                        .withLabels(Map.of(ResourceLabels.STRIMZI_CLUSTER_LABEL, NAME))
                    .endMetadata()
                    .withNewSpec()
                        .withReplicas(3)
                        .withRoles(ProcessRoles.CONTROLLER, ProcessRoles.BROKER)
                        .withNewEphemeralStorage()
                        .endEphemeralStorage()
                    .endSpec()
                    .build();

            LOGGER.info("Creating the Kafka Node Pool");
            Crds.kafkaNodePoolOperation(client).inNamespace(NAMESPACE).resource(pool).create();

            Kafka kafka = new KafkaBuilder()
                    .withNewMetadata()
                        .withName(NAME)
                        .withNamespace(NAMESPACE)
                        .withAnnotations(Map.of(ResourceAnnotations.ANNO_STRIMZI_IO_KRAFT, "enabled", ResourceAnnotations.ANNO_STRIMZI_IO_NODE_POOLS, "enabled"))
                    .endMetadata()
                    .withNewSpec()
                        .withNewKafka()
                            .withListeners(new GenericKafkaListenerBuilder()
                                    .withName("plain")
                                    .withType(KafkaListenerType.INTERNAL)
                                    .withPort(9092)
                                    .withTls(false)
                                    .build())
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
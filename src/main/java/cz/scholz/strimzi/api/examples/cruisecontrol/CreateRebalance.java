package cz.scholz.strimzi.api.examples.cruisecontrol;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.KafkaRebalance;
import io.strimzi.api.kafka.model.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.balancing.KafkaRebalanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class CreateRebalance {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateRebalance.class);
    private static final String NAMESPACE = "myproject";
    private static final String REBALANCE_NAME = "myrebalance";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            KafkaRebalance rebalance = new KafkaRebalanceBuilder()
                    .withNewMetadata()
                        .withName(REBALANCE_NAME)
                        .addToLabels("strimzi.io/cluster", "my-cluster")
                        .withClusterName("my-cluster")
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();

            LOGGER.info("create rebalance");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).resource(rebalance).create();

            LOGGER.info("wait for rebalance proposal ready");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).withName(REBALANCE_NAME)
                    .waitUntilCondition(KafkaRebalance.isInState(KafkaRebalanceState.ProposalReady), 5, TimeUnit.MINUTES);

            KafkaRebalance approvedRebalance = new KafkaRebalanceBuilder()
                    .withNewMetadata()
                        .withName(REBALANCE_NAME)
                        .addToLabels("strimzi.io/cluster", "my-cluster")
                        .withClusterName("my-cluster")
                        .addToAnnotations("strimzi.io/rebalance", "approve")
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();

            LOGGER.info("annotate rebalance as approved");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).resource(approvedRebalance).createOrReplace();

            LOGGER.info("wait for rebalance to finish");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).withName(REBALANCE_NAME)
                    .waitUntilCondition(KafkaRebalance.isInState(KafkaRebalanceState.Ready), 5, TimeUnit.MINUTES);
            LOGGER.info("rebalance finished");
        }
    }
}
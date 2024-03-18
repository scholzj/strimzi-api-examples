package cz.scholz.strimzi.api.examples.cruisecontrol;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Rebalance {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rebalance.class);
    private static final String NAMESPACE = "myproject";
    private static final String REBALANCE_NAME = "myrebalance";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            KafkaRebalance rebalance = new KafkaRebalanceBuilder()
                    .withNewMetadata()
                        .withName(REBALANCE_NAME)
                        .addToLabels("strimzi.io/cluster", "my-cluster")
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();

            LOGGER.info("Creating KafkaRebalance resource");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).resource(rebalance).create();

            LOGGER.info("Waiting for the rebalance proposal to be ready");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).withName(REBALANCE_NAME)
                    .waitUntilCondition(KafkaRebalance.isInState(KafkaRebalanceState.ProposalReady), 5, TimeUnit.MINUTES);

            KafkaRebalance approvedRebalance = new KafkaRebalanceBuilder()
                    .withNewMetadata()
                        .withName(REBALANCE_NAME)
                        .addToLabels("strimzi.io/cluster", "my-cluster")
                        .addToAnnotations("strimzi.io/rebalance", "approve")
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();

            LOGGER.info("Approve the rebalance");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).resource(approvedRebalance).update();

            LOGGER.info("Waiting for rebalance to finish");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).withName(REBALANCE_NAME)
                    .waitUntilCondition(KafkaRebalance.isInState(KafkaRebalanceState.Ready), 5, TimeUnit.MINUTES);

            LOGGER.info("Rebalance is finished - deleting the KafkaRebalance resource");
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).withName(REBALANCE_NAME).delete();
        }
    }
}
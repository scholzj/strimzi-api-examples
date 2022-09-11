package cz.scholz.strimzi.api.examples.cruisecontrol;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.KafkaRebalance;
import io.strimzi.api.kafka.model.KafkaRebalanceBuilder;

public class DeleteRebalance {
    private static final String NAMESPACE = "myproject";
    private static final String REBALANCE_NAME = "myrebalance";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            KafkaRebalance rebalance = new KafkaRebalanceBuilder()
                    .editMetadata()
                        .withName(REBALANCE_NAME)
                        .addToLabels("strimzi.io/cluster", "my-cluster")
                        .withClusterName("my-cluster")
                    .endMetadata()
                    .withNewSpec()
                    .endSpec()
                    .build();
            Crds.kafkaRebalanceOperation(client).inNamespace(NAMESPACE).resource(rebalance).delete();
        }
    }
}
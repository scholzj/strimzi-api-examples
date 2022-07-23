package cz.scholz.strimzi.api.examples.kafka;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UpdateKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateKafka.class);
    private static final String NAMESPACE = "myproject";
    private static final String NAME = "my-cluster";

    public static void main(String[] args) {
        try (KubernetesClient client = new DefaultKubernetesClient()) {
            LOGGER.info("Reconfiguring the Kafka cluster (disable topic auto-creation)");
            final Kafka updatedKafka = Crds.kafkaOperation(client).inNamespace(NAMESPACE).withName(NAME)
                    .edit(k -> new KafkaBuilder(k)
                            .editSpec()
                                .editKafka()
                                    .withConfig(Map.of("auto.create.topics.enable", "false"))
                                .endKafka()
                            .endSpec()
                            .build());

            LOGGER.info("Waiting for the cluster to be updated");
            Crds.kafkaOperation(client).inNamespace(NAMESPACE).withName(NAME).waitUntilCondition(k -> {
                if (k.getStatus() != null && k.getStatus().getConditions() != null) {
                    return updatedKafka.getMetadata().getGeneration() == k.getStatus().getObservedGeneration()
                            && k.getStatus().getConditions().stream().anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
                } else {
                    return false;
                }
            }, 5, TimeUnit.MINUTES);

            LOGGER.info("Kafka cluster was updated");
        }
    }
}
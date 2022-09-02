package cz.scholz.strimzi.api.examples.connect;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnpauseConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnpauseConnector.class);
    private static final String NAMESPACE = "myproject";
    private static final String TIMER_CONNECTOR_NAME = "timer-connector";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Pausing the connector");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(TIMER_CONNECTOR_NAME)
                    .edit(c -> new KafkaConnectorBuilder(c)
                            .editSpec()
                                .withPause(false)
                            .endSpec()
                            .build());
        }
    }
}
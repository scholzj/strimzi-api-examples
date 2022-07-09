package cz.scholz.strimzi.api.examples;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaExample.class);
    private static final String NAMESPACE = "myproject";
    private static final String NAME = "my-connector";

    public static void main(String[] args) {
        KubernetesClient client = new DefaultKubernetesClient();

        LOGGER.info("Pausing the connector");
        Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(NAME)
                .edit(c -> new KafkaConnectorBuilder(c)
                        .editSpec()
                            .withPause(true)
                        .endSpec()
                        .build());

        client.close();
    }
}
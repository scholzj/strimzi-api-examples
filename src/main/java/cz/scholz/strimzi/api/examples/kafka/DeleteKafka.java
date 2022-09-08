package cz.scholz.strimzi.api.examples.kafka;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteKafka {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteKafka.class);
    private static final String NAMESPACE = "myproject";
    private static final String NAME = "my-cluster";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Deleting the cluster");
            Crds.kafkaOperation(client).inNamespace(NAMESPACE).withName(NAME).delete();
        }
    }
}
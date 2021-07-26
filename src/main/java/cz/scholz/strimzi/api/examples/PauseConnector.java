package cz.scholz.strimzi.api.examples;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.KafkaConnectorList;
import io.strimzi.api.kafka.model.KafkaConnector;

public class PauseConnector {
    public static void main(String[] args) {
        String namespace = "myproject";
        String crName = "my-connector";

        KubernetesClient client = new DefaultKubernetesClient();
        MixedOperation<KafkaConnector, KafkaConnectorList, Resource<KafkaConnector>> op = Crds.kafkaConnectorOperation(client);
        KafkaConnector connector = op.inNamespace(namespace).withName(crName).get();
        connector.getSpec().setPause(true);
        op.inNamespace(namespace).withName(crName).replace(connector);

        client.close();
    }
}
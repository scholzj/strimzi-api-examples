package cz.scholz.strimzi.api.examples.connect;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.KafkaConnect;
import io.strimzi.api.kafka.model.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.KafkaConnector;
import io.strimzi.api.kafka.model.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.KafkaTopicBuilder;
import io.strimzi.api.kafka.model.connect.build.JarArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.PluginBuilder;
import io.strimzi.api.kafka.model.connect.build.TgzArtifactBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CreateConnectAndConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateConnectAndConnector.class);
    private static final String NAMESPACE = "myproject";
    private static final String KAFKA_NAME = "my-cluster";
    private static final String CONNECT_NAME = "my-connect";
    private static final String TOPIC_NAME = "my-topic";
    private static final String ECHO_CONNECTOR_NAME = "echo-connector";
    private static final String TIMER_CONNECTOR_NAME = "timer-connector";

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            Map<String, Object> connectConfig = new HashMap<>();
            connectConfig.put("group.id", "connect-cluster");
            connectConfig.put("offset.storage.topic", "connect-cluster-offsets");
            connectConfig.put("config.storage.topic", "connect-cluster-configs");
            connectConfig.put("status.storage.topic", "connect-cluster-status");
            connectConfig.put("config.storage.replication.factor", -1);
            connectConfig.put("offset.storage.replication.factor", -1);
            connectConfig.put("status.storage.replication.factor", -1);
            connectConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
            connectConfig.put("value.converter", "org.apache.kafka.connect.storage.StringConverter");
            connectConfig.put("key.converter.schemas.enable", false);
            connectConfig.put("value.converter.schemas.enable", false);

            KafkaConnect connect = new KafkaConnectBuilder()
                    .withNewMetadata()
                        .withName(CONNECT_NAME)
                        .withNamespace(NAMESPACE)
                        .withAnnotations(Map.of("strimzi.io/use-connector-resources", "true"))
                    .endMetadata()
                    .withNewSpec()
                        .withReplicas(1)
                        .withBootstrapServers("my-cluster-kafka-bootstrap:9092")
                        .withConfig(connectConfig)
                        .withNewBuild()
                            .withNewDockerOutput()
                                .withImage("ttl.sh/strimzi-api-examples:24h")
                            .endDockerOutput()
                            .withPlugins(new PluginBuilder().withName("echo-plugin").withArtifacts(new JarArtifactBuilder().withUrl("https://github.com/scholzj/echo-sink/releases/download/1.6.0/echo-sink-1.6.0.jar").build()).build(),
                                    new PluginBuilder().withName("timer-plugin").withArtifacts(new TgzArtifactBuilder().withUrl("https://repo1.maven.org/maven2/org/apache/camel/kafkaconnector/camel-timer-kafka-connector/0.11.5/camel-timer-kafka-connector-0.11.5-package.tar.gz").build()).build())
                        .endBuild()
                    .endSpec()
                    .build();

            LOGGER.info("Creating the Kafka Connect cluster");
            Crds.kafkaConnectOperation(client).inNamespace(NAMESPACE).resource(connect).create();

            LOGGER.info("Waiting for the Connect cluster to be ready");
            Crds.kafkaConnectOperation(client).inNamespace(NAMESPACE).withName(CONNECT_NAME).waitUntilCondition(KafkaConnect.isReady(), 10, TimeUnit.MINUTES);

            LOGGER.info("Connect cluster is ready");

            KafkaTopic topic = new KafkaTopicBuilder()
                    .withNewMetadata()
                        .withName(TOPIC_NAME)
                        .withNamespace(NAMESPACE)
                        .withLabels(Map.of("strimzi.io/cluster", KAFKA_NAME))
                    .endMetadata()
                    .withNewSpec()
                        .withPartitions(3)
                        .withReplicas(3)
                        .withConfig(Map.of("retention.ms", 7200000,
                                "segment.bytes", 107374182,
                                "min.insync.replicas", 2))
                    .endSpec()
                    .build();

            LOGGER.info("Creating the topic");
            Crds.topicOperation(client).inNamespace(NAMESPACE).resource(topic).create();

            LOGGER.info("Waiting for the topic to be ready");
            Crds.topicOperation(client).inNamespace(NAMESPACE).withName(TOPIC_NAME).waitUntilCondition(KafkaTopic.isReady(), 5, TimeUnit.MINUTES);

            LOGGER.info("Topic is ready");

            KafkaConnector echoConnector = new KafkaConnectorBuilder()
                    .withNewMetadata()
                        .withName(ECHO_CONNECTOR_NAME)
                        .withNamespace(NAMESPACE)
                        .withLabels(Map.of("strimzi.io/cluster", CONNECT_NAME))
                    .endMetadata()
                    .withNewSpec()
                        .withClassName("EchoSink")
                        .withTasksMax(3)
                        .withConfig(Map.of("level", "INFO",
                                "topics", "my-topic"))
                    .endSpec()
                    .build();

            LOGGER.info("Creating the Echo connector");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).resource(echoConnector).create();

            LOGGER.info("Waiting for the Echo connector to be ready");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(ECHO_CONNECTOR_NAME).waitUntilCondition(KafkaConnector.isReady(), 5, TimeUnit.MINUTES);

            LOGGER.info("Echo connector is ready");

            Map<String, Object> timerConnectorConfig = new HashMap<>();
            timerConnectorConfig.put("topics", "my-topic");
            timerConnectorConfig.put("camel.source.path.timerName", "timer");
            timerConnectorConfig.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
            timerConnectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
            timerConnectorConfig.put("value.converter.schemas.enable", false);
            timerConnectorConfig.put("transforms", "HoistField,InsertField,ReplaceField");
            timerConnectorConfig.put("transforms.HoistField.type", "org.apache.kafka.connect.transforms.HoistField$Value");
            timerConnectorConfig.put("transforms.HoistField.field", "originalValue");
            timerConnectorConfig.put("transforms.InsertField.type", "org.apache.kafka.connect.transforms.InsertField$Value");
            timerConnectorConfig.put("transforms.InsertField.timestamp.field", "timestamp");
            timerConnectorConfig.put("transforms.InsertField.static.field", "message");
            timerConnectorConfig.put("transforms.InsertField.static.value", "Hello World");
            timerConnectorConfig.put("transforms.ReplaceField.type", "org.apache.kafka.connect.transforms.ReplaceField$Value");
            timerConnectorConfig.put("transforms.ReplaceField.blacklist", "originalValue");

            KafkaConnector timerConnector = new KafkaConnectorBuilder()
                    .withNewMetadata()
                        .withName(TIMER_CONNECTOR_NAME)
                        .withNamespace(NAMESPACE)
                        .withLabels(Map.of("strimzi.io/cluster", CONNECT_NAME))
                    .endMetadata()
                    .withNewSpec()
                        .withClassName("CamelTimerSourceConnector")
                        .withTasksMax(1)
                        .withConfig(timerConnectorConfig)
                    .endSpec()
                    .build();

            LOGGER.info("Creating the Timer connector");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).resource(timerConnector).create();

            LOGGER.info("Waiting for the Timer connector to be ready");
            Crds.kafkaConnectorOperation(client).inNamespace(NAMESPACE).withName(TIMER_CONNECTOR_NAME).waitUntilCondition(KafkaConnector.isReady(), 5, TimeUnit.MINUTES);

            LOGGER.info("Timer connector is ready");
        }
    }
}
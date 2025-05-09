package cz.scholz.strimzi.api.examples.connect;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connect.KafkaConnectBuilder;
import io.strimzi.api.kafka.model.connect.build.JarArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.MavenArtifactBuilder;
import io.strimzi.api.kafka.model.connect.build.PluginBuilder;
import io.strimzi.api.kafka.model.connector.KafkaConnector;
import io.strimzi.api.kafka.model.connector.KafkaConnectorBuilder;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicBuilder;
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
                                .withAdditionalKanikoOptions("--ignore-path=/usr/bin/newuidmap", "--ignore-path=/usr/bin/newgidmap")
                            .endDockerOutput()
                            .withPlugins(new PluginBuilder().withName("echo-plugin").withArtifacts(new JarArtifactBuilder().withUrl("https://github.com/scholzj/echo-sink/releases/download/1.6.0/echo-sink-1.6.0.jar").build()).build(),
                                    new PluginBuilder().withName("timer-plugin").withArtifacts(new MavenArtifactBuilder().withGroup("org.apache.camel.kafkaconnector").withArtifact("camel-timer-source-kafka-connector").withVersion("4.10.3").build()).build())
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
            timerConnectorConfig.put("camel.kamelet.timer-source.period", "1000");
            timerConnectorConfig.put("camel.kamelet.timer-source.message", "Hello World");

            KafkaConnector timerConnector = new KafkaConnectorBuilder()
                    .withNewMetadata()
                        .withName(TIMER_CONNECTOR_NAME)
                        .withNamespace(NAMESPACE)
                        .withLabels(Map.of("strimzi.io/cluster", CONNECT_NAME))
                    .endMetadata()
                    .withNewSpec()
                        .withClassName("CamelTimersourceSourceConnector")
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
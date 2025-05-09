package cz.scholz.strimzi.api.examples.installation;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Install {
    private static final Logger LOGGER = LoggerFactory.getLogger(Install.class);

    protected static final String STRIMZI_VERSION = "0.46.0";

    private static final String INSTALLATION_YAML = "https://github.com/strimzi/strimzi-kafka-operator/releases/download/" + STRIMZI_VERSION + "/strimzi-cluster-operator-" + STRIMZI_VERSION + ".yaml";
    private static final String OPERATOR_NAMESPACE = "strimzi";
    private static final boolean WATCH_ALL_NAMESPACES = true; // When set to true, the operator will watch all namespaces. When set to false, it will watch only the OPERATOR_NAMESPACE namespace

    public static void main(String[] args) {
        try (KubernetesClient client = new KubernetesClientBuilder().build()) {
            LOGGER.info("Checking if namespace {} exists", OPERATOR_NAMESPACE);
            if (client.namespaces().withName(OPERATOR_NAMESPACE).get() == null) {
                LOGGER.info("Creating namespace {}", OPERATOR_NAMESPACE);
                client.namespaces().resource(new NamespaceBuilder().withNewMetadata().withName(OPERATOR_NAMESPACE).endMetadata().build()).create();
            }

            LOGGER.info("Loading installation files from {}", INSTALLATION_YAML);
            List<HasMetadata> resources = client.load(new BufferedInputStream(new URL(INSTALLATION_YAML).openStream())).items();

            for (HasMetadata resource : resources) {
                if (resource instanceof ServiceAccount) {
                    LOGGER.info("Creating {} named {} in namespace {}", resource.getKind(), resource.getMetadata().getName(), OPERATOR_NAMESPACE);
                    resource.getMetadata().setNamespace(OPERATOR_NAMESPACE);
                    ServiceAccount sa = (ServiceAccount) resource;
                    client.serviceAccounts().inNamespace(OPERATOR_NAMESPACE).resource(sa).create();
                } else if (resource instanceof ClusterRole) {
                    LOGGER.info("Creating {} named {}", resource.getKind(), resource.getMetadata().getName());
                    ClusterRole cr = (ClusterRole) resource;
                    client.rbac().clusterRoles().resource(cr).create();
                } else if (resource instanceof ClusterRoleBinding) {
                    LOGGER.info("Creating {} named {}", resource.getKind(), resource.getMetadata().getName());
                    ClusterRoleBinding crb = (ClusterRoleBinding) resource;
                    crb.getSubjects().forEach(sbj -> sbj.setNamespace(OPERATOR_NAMESPACE));
                    client.rbac().clusterRoleBindings().resource(crb).create();
                } else if (resource instanceof RoleBinding) {
                    resource.getMetadata().setNamespace(OPERATOR_NAMESPACE);
                    RoleBinding rb = (RoleBinding) resource;
                    rb.getSubjects().forEach(sbj -> sbj.setNamespace(OPERATOR_NAMESPACE));

                    if ("strimzi-cluster-operator-leader-election".equals(rb.getMetadata().getName()))  {
                        // The Leader Election RoleBinding is always needed only in the Cluster Operator namespace
                        LOGGER.info("Creating {} named {} in namespace {}", resource.getKind(), resource.getMetadata().getName(), OPERATOR_NAMESPACE);
                        client.rbac().roleBindings().inNamespace(OPERATOR_NAMESPACE).resource(rb).create();
                    } else if (WATCH_ALL_NAMESPACES) {
                        ClusterRoleBinding crb = new ClusterRoleBindingBuilder()
                                .withNewMetadata()
                                .withName(rb.getMetadata().getName() + "-all-ns")
                                .withAnnotations(rb.getMetadata().getAnnotations())
                                .withLabels(rb.getMetadata().getLabels())
                                .endMetadata()
                                .withRoleRef(rb.getRoleRef())
                                .withSubjects(rb.getSubjects())
                                .build();

                        LOGGER.info("Creating {} named {}", crb.getKind(), crb.getMetadata().getName());
                        client.rbac().clusterRoleBindings().resource(crb).create();
                    } else {
                        // RoleBindings not related to leader election (but related to operands) need to be installed to
                        // watched namespace(s). In this example, the Cluster Operator is configured to watch its own
                        // namespace only. In case you want to watch it other namespace(s), these RoleBindings need to
                        // be created there.
                        LOGGER.info("Creating {} named {} in namespace {}", resource.getKind(), resource.getMetadata().getName(), OPERATOR_NAMESPACE);
                        client.rbac().roleBindings().inNamespace(OPERATOR_NAMESPACE).resource(rb).create();
                    }
                } else if (resource instanceof CustomResourceDefinition) {
                    LOGGER.info("Creating {} named {}", resource.getKind(), resource.getMetadata().getName());
                    CustomResourceDefinition crd = (CustomResourceDefinition) resource;
                    client.apiextensions().v1().customResourceDefinitions().resource(crd).create();
                } else if (resource instanceof ConfigMap) {
                    LOGGER.info("Creating {} named {} in namespace {}", resource.getKind(), resource.getMetadata().getName(), OPERATOR_NAMESPACE);
                    resource.getMetadata().setNamespace(OPERATOR_NAMESPACE);
                    ConfigMap cm = (ConfigMap) resource;
                    client.configMaps().inNamespace(OPERATOR_NAMESPACE).resource(cm).create();
                } else if (resource instanceof Deployment) {
                    LOGGER.info("Creating {} named {} in namespace {}", resource.getKind(), resource.getMetadata().getName(), OPERATOR_NAMESPACE);
                    resource.getMetadata().setNamespace(OPERATOR_NAMESPACE);
                    Deployment dep = (Deployment) resource;

                    if (WATCH_ALL_NAMESPACES) {
                        Container container = dep.getSpec().getTemplate().getSpec().getContainers().stream().filter(cont -> "strimzi-cluster-operator".equals(cont.getName())).findFirst().orElseThrow();
                        EnvVar envVar = container.getEnv().stream().filter(env -> "STRIMZI_NAMESPACE".equals(env.getName())).findFirst().orElseThrow();
                        envVar.setValueFrom(null);
                        envVar.setValue("*");
                    }

                    client.apps().deployments().inNamespace(OPERATOR_NAMESPACE).resource(dep).create();
                } else {
                    LOGGER.info("Unknown resource {} named {}", resource.getKind(), resource.getMetadata().getName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Something went wrong", e);
        }
    }
}
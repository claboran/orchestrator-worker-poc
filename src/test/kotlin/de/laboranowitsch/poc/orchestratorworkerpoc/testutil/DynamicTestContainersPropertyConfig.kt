package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.Container
import org.testcontainers.containers.GenericContainer

@TestConfiguration
class DynamicTestContainersPropertyConfig {
    @Bean
    fun registrar(containers: List<Container<*>>) = DynamicPropertyRegistrar {
        containers.forEach { container ->
            when (container) {
                // We intentionally treat GenericContainer instances here because Spring's @ServiceConnection
                // handles known container types (e.g. PostgreSQLContainer) automatically. This registrar
                // only needs to configure generic/uncategorized containers such as our ElasticMQ GenericContainer.
                is GenericContainer<*> -> {
                    when {
                        container.labels.values.contains(TestContainerConfig.ELASTIC_MQ_CONTAINER_NAME) -> {
                            it.add("spring.cloud.aws.sqs.endpoint") {
                                container.getSqsUrl(TestContainerConfig.ELASTIC_MQ_PORT)
                            }
                            it.add("spring.cloud.aws.credentials.access-key") { TestContainerConfig.SQS_ACCESS_KEY }
                            it.add("spring.cloud.aws.credentials.secret-key") { TestContainerConfig.SQS_SECRET_KEY }
                            it.add("spring.cloud.aws.region.static") { TestContainerConfig.SQS_REGION }
                        }
                    }
                }
            }
        }
    }
}

package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestContainerConfig {

    @Bean
    @ServiceConnection // This annotation handles all PostgreSQL connection properties for you!
    fun postgresContainer(): PostgreSQLContainer<*> = PostgreSQLContainer(POSTGRES_IMAGE).apply {
        withDatabaseName(POSTGRES_DB)
        withUsername(POSTGRES_USER)
        withPassword(POSTGRES_PASSWORD)
    }

    @Bean
    fun elasticMqContainer(): GenericContainer<*> = GenericContainer(
        DockerImageName.parse(ELASTIC_MQ_IMAGE),
    ).apply {
        withLabel(ELASTIC_MQ_CONTAINER_NAME, ELASTIC_MQ_CONTAINER_NAME)
        withExposedPorts(ELASTIC_MQ_PORT)

        // Create elasticmq.conf directly inside the container and start ElasticMQ
        withCommand("sh", "-c", ELASTIC_MQ_COMMAND)

        waitingFor(Wait.forListeningPort())
    }


    companion object {
        const val POSTGRES_IMAGE = "postgres:16"
        const val POSTGRES_DB = "testdb"
        const val POSTGRES_USER = "test"
        const val POSTGRES_PASSWORD = "test"

        const val ELASTIC_MQ_IMAGE = "softwaremill/elasticmq-native:1.6.15"
        const val ELASTIC_MQ_CONTAINER_NAME = "elastic-mq-test-container"
        const val ELASTIC_MQ_PORT = 9324

        // Non-const because it's a multiline string
        val ELASTIC_MQ_COMMAND: String = """
            cat > /tmp/elasticmq.conf << 'EOF'
            include "application.conf"

            queues {
              # The queue for high-level job start commands
              job-control-queue {
                defaultVisibilityTimeout = 60 seconds
              }

              # The queue for the actual page-by-page work
              job-worker-queue {
                defaultVisibilityTimeout = 30 seconds
              }
            }
            EOF
            exec java -Dconfig.file=/tmp/elasticmq.conf -jar /opt/elasticmq.jar
            """.trimIndent()

        const val SQS_ACCESS_KEY = "test"
        const val SQS_SECRET_KEY = "test"
        const val SQS_REGION = "us-east-1"
    }
}

fun GenericContainer<*>.getSqsUrl(originalPort: Int) =
    "http://${this.host}:${this.getMappedPort(originalPort)}"

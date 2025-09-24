package de.laboranowitsch.poc.orchestratorworkerpoc

import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTest
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTestContainers
import org.junit.jupiter.api.Test

@IntegrationTest
class OrchestratorWorkerPocApplicationTests : IntegrationTestContainers() {

    @Test
    fun contextLoads() {
        // This test verifies that the Spring application context can be loaded successfully
        // with all required services (PostgreSQL and ElasticMQ) provided by testcontainers
    }

}

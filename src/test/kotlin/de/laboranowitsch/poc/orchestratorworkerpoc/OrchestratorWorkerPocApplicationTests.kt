package de.laboranowitsch.poc.orchestratorworkerpoc

import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.junit.jupiter.api.Test

@IntegrationTests
class OrchestratorWorkerPocApplicationTests  {

    @Test
    fun contextLoads() {
        // This test verifies that the Spring application context can be loaded successfully
        // with all required services (PostgreSQL and ElasticMQ) provided by testcontainers
    }

}

package de.laboranowitsch.poc.orchestratorworkerpoc.state

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class JobStateRepositoryIntegrationTest @Autowired constructor(
    private val repo: JobStateRepository,
) {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }

    @Test
    fun `job state repository saves and finds`() {
        val js = JobState(jobId = "job-123", status = "NEW", payload = "{\"foo\":\"bar\"}")
        val saved = repo.save(js)

        val found = repo.findByJobId("job-123")
        assertThat(found).isNotNull
        assertThat(found?.jobId).isEqualTo("job-123")
        assertThat(found?.status).isEqualTo("NEW")
    }
}

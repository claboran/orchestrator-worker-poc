package de.laboranowitsch.poc.orchestratorworkerpoc.state

import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTests
class JobStateRepositoryIntegrationTest @Autowired constructor(
    private val repo: JobStateRepository,
) {

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

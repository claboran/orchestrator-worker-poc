package de.laboranowitsch.poc.orchestratorworkerpoc.repository

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobState
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.*

@IntegrationTests
class JobStateRepositoryIntegrationTest @Autowired constructor(
    private val repo: JobStateRepository,
) {

    @BeforeEach
    fun setup() {
        repo.save(createJobState())
    }

    @AfterEach
    fun cleanup() {
        repo.deleteAll()
    }

    @Test
    fun `job state repository saves and finds`() {
        val jobState = repo.findByIdOrNull(JOB_ID)

        with(jobState!!) {
            assertThat(id).isEqualTo(JOB_ID)
            assertThat(status).isEqualTo(JobStatus.CREATED)
        }
    }

    @Test
    fun `job state repository updates payload and timestamps`() {
        val jobState = repo.findByIdOrNull(JOB_ID)!!
        jobState.status = JobStatus.FINISHED
    }

    companion object {
        val JOB_ID: UUID = UUID.randomUUID()

        @JvmStatic
        fun createJobState() = JobState(
            id = JOB_ID,
            status = JobStatus.CREATED,
        )
    }
}

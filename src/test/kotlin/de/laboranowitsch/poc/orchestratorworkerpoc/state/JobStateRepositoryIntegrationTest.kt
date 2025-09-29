package de.laboranowitsch.poc.orchestratorworkerpoc.state

import com.fasterxml.jackson.databind.ObjectMapper
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTests
class JobStateRepositoryIntegrationTest @Autowired constructor(
    private val repo: JobStateRepository,
) {

    private val mapper = ObjectMapper()

    @Test
    fun `job state repository saves and finds`() {
        val js = JobState(jobId = "job-123", status = JobStatus.CREATED, payload = "{\"foo\":\"bar\"}")
        val saved = repo.save(js)

        val found = repo.findByJobId("job-123")
        with(found) {
            assertThat(this).isNotNull
            assertThat(this?.jobId).isEqualTo("job-123")
            assertThat(this?.status).isEqualTo(JobStatus.CREATED)
        }

        // also assert on the returned saved instance to avoid unused variable warning
        assertThat(saved.jobId).isEqualTo("job-123")

        // Compare JSON structurally to avoid whitespace/formatting differences
        val expectedNode = mapper.readTree("{\"foo\":\"bar\"}")
        val actualNode = mapper.readTree(found?.payload ?: "null")
        assertThat(actualNode).isEqualTo(expectedNode)
    }

    @Test
    fun `job state repository updates status`() {
        val js = JobState(jobId = "job-update-1", status = JobStatus.CREATED, payload = "{}")
        val saved = repo.save(js)

        // sanity
        assertThat(saved.status).isEqualTo(JobStatus.CREATED)

        // update status
        saved.status = JobStatus.RUNNING
        val updated = repo.save(saved)

        // assert using the returned updated instance to avoid unused variable warning
        assertThat(updated.status).isEqualTo(JobStatus.RUNNING)

        val found = repo.findByJobId("job-update-1")
        assertThat(found).isNotNull
        assertThat(found?.status).isEqualTo(JobStatus.RUNNING)
    }

    @Test
    fun `job state repository updates payload and timestamps`() {
        val js = JobState(jobId = "job-update-2", status = JobStatus.CREATED, payload = "{\"a\":1}")
        val saved = repo.save(js)

        val beforeUpdatedAt = saved.updatedAt!!

        // ensure timestamp will change
        Thread.sleep(120)

        // mutate payload and status and save
        saved.payload = "{\"a\":2}"
        saved.status = JobStatus.FINISHED
        val updated = repo.save(saved)

        val found = repo.findByJobId("job-update-2")
        assertThat(found).isNotNull

        // Compare JSON structurally so formatting differences (spaces) don't fail the test
        val expectedNode = mapper.readTree("{\"a\":2}")
        val actualNode = mapper.readTree(found?.payload ?: "null")
        assertThat(actualNode).isEqualTo(expectedNode)

        assertThat(found?.status).isEqualTo(JobStatus.FINISHED)
        assertThat(found?.updatedAt).isNotNull
        // use the returned updated instance in assertion to avoid unused variable warning
        assertThat(updated.updatedAt).isAfter(beforeUpdatedAt)
    }
}

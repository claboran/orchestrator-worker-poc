package de.laboranowitsch.poc.orchestratorworkerpoc.state

import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

@IntegrationTests
class PageStateRepositoryIntegrationTest @Autowired constructor(
    private val jobRepo: JobStateRepository,
    private val pageRepo: PageStateRepository,
) {

    @Test
    fun `save and retrieve page_state linked to job`() {
        // create and save job
        val job = JobState(jobId = "job-with-pages")
        val savedJob = jobRepo.save(job)

        // create page data
        val itemId = UUID.randomUUID()
        val pd = PageData(itemIds = listOf(itemId))

        // create and save page, id generated in code
        val page = PageState(id = UUID.randomUUID(), data = pd, jobState = savedJob)
        val savedPage = pageRepo.save(page)

        val found = pageRepo.findByJobStateJobId("job-with-pages")
        assertThat(found).isNotNull
        assertThat(found).hasSize(1)
        val p = found.first()
        assertThat(p.id).isEqualTo(savedPage.id)
        // jobState is nullable in the entity (to allow JPA no-arg). Use null-safe access in test.
        assertThat(p.jobState!!.jobId).isEqualTo("job-with-pages")
        assertThat(p.data).isNotNull
        assertThat(p.data!!.itemIds).containsExactly(itemId)
    }

    @Test
    fun `save page via job add and cascade`() {
        val job = JobState(jobId = "job-cascade")
        val itemId = UUID.randomUUID()
        val pd = PageData(itemIds = listOf(itemId))

        // use convenience method to create and attach page
        job.addPage(pd)

        val savedJob = jobRepo.save(job)

        val pages = pageRepo.findByJobStateJobId("job-cascade")
        assertThat(pages).isNotNull
        assertThat(pages).hasSize(1)
        val p = pages.first()
        assertThat(p.jobState!!.jobId).isEqualTo("job-cascade")
        assertThat(p.data!!.itemIds).containsExactly(itemId)
    }
}

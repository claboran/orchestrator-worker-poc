package de.laboranowitsch.poc.orchestratorworkerpoc.repository

import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus.CREATED
import de.laboranowitsch.poc.orchestratorworkerpoc.data.PageStatus.RUNNING
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobState
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageData
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageState
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

@IntegrationTests
class PageStateRepositoryIntegrationTest @Autowired constructor(
    private val jobRepo: JobStateRepository,
    private val pageRepo: PageStateRepository,
) {
    @BeforeEach
    fun setup() {
        jobRepo.save(createJobState())
    }

    @AfterEach
    fun cleanup() {
        jobRepo.deleteAll()
    }

    @Test
    fun `retrieve page_state linked to job`() {
        val pageStates = pageRepo.findByJobStateId(JOB_ID)

        assertThat(pageStates).isNotNull
        assertThat(pageStates).hasSize(1)

        with(pageStates.first()) {
            assertThat(id).isEqualTo(PAGED_ID)
            assertThat(jobState!!.id).isEqualTo(JOB_ID)
            assertThat(data).isNotNull
            assertThat(data!!.itemIds.size).isEqualTo(UUIDS_PER_PAGE)
        }
    }

    @Test
    fun `update PageState with new status`() {
        val pageStates = pageRepo.findByJobStateId(JOB_ID)
        val pageState = pageStates.first()
        pageState.status = RUNNING
        pageRepo.save(pageState)
        val updatedPageState = pageRepo.findByJobStateId(JOB_ID).first()
        assertThat(updatedPageState.status).isEqualTo(RUNNING)
    }

    @Test
    fun `created and updated timestamps are set and updated`() {
        val pageStates = pageRepo.findByJobStateId(JOB_ID)
        val pageState = pageStates.first()

        val createdAtInitial = pageState.createdAt
        val updatedAtInitial = pageState.updatedAt

        assertThat(createdAtInitial).isNotNull
        assertThat(updatedAtInitial).isNull()

        // modify and save to trigger update timestamp change
        pageState.status = RUNNING
        pageRepo.save(pageState)

        val updatedPageState = pageRepo.findByJobStateId(JOB_ID).first()
        val updatedAtAfter = updatedPageState.updatedAt

        assertThat(updatedAtAfter).isNotNull
        assertThat(updatedAtAfter).isAfter(createdAtInitial)
    }

    companion object {
        private val JOB_ID: UUID = UUID.randomUUID()
        private val PAGED_ID: UUID = UUID.randomUUID()
        private const val UUIDS_PER_PAGE = 3
        private val PAGE_DATA = PageData(
            itemIds = List(UUIDS_PER_PAGE.coerceAtLeast(0)) { UUID.randomUUID() },
        )

        @JvmStatic
        fun createJobState() = JobState(
            id = JOB_ID,
            status = JobStatus.CREATED,
        ).apply {
            addPage(
                PageState(
                    id = PAGED_ID,
                    status = CREATED,
                    data = PAGE_DATA,
                )
            )
        }
    }
}

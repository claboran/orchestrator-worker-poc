package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.repository.PageStateRepository
import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageStatus
import de.laboranowitsch.poc.orchestratorworkerpoc.testutil.IntegrationTests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@IntegrationTests
@TestPropertySource(properties = [
    "app.pagePayload.numberOfPages=4",
    "app.pagePayload.uuidsPerPage=6",
])
class PocPagePayloadServiceIntegrationTest @Autowired constructor(
    private val pageRepo: PageStateRepository,
    private val service: PocPagePayloadService,
) {

    @Test
    fun `generates configured pages with configured uuids and persists them`() {
        with(service.generateForJob(JOB_ID)!!) {
            assertThat(id).isEqualTo(JOB_ID)
            assertThat(status).isEqualTo(JobStatus.CREATED)
        }

        pageRepo.findByJobStateId(JOB_ID).let {
            assertThat(it).hasSize(EXPECTED_PAGES)
            assertThat(it).allSatisfy { p ->
                assertThat(p.jobState?.id).isEqualTo(JOB_ID)
                assertThat(p.status).isEqualTo(PageStatus.CREATED)
                assertThat(p.data).isNotNull
                assertThat(p.data!!.itemIds).hasSize(EXPECTED_UUIDS_PER_PAGE)
            }
        }
    }

    companion object {
        val JOB_ID: UUID = UUID.randomUUID()
        const val EXPECTED_PAGES: Int = 4
        const val EXPECTED_UUIDS_PER_PAGE: Int = 6
    }
}

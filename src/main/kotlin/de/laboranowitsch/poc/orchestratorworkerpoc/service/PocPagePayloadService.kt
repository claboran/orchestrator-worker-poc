package de.laboranowitsch.poc.orchestratorworkerpoc.service

import de.laboranowitsch.poc.orchestratorworkerpoc.state.*
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class PocPagePayloadService(
    @param:Value("\${app.page-payload.number-of-pages:1}") private val numberOfPages: Int,
    @param:Value("\${app.page-payload.uuids-per-page:1}") private val uuidsPerPage: Int,
    private val jobStateRepository: JobStateRepository,
) : LoggingAware {

    @Transactional
    fun generateForJob(jobId: String): JobState? =
        (0 until numberOfPages.coerceAtLeast(0))
            .fold(
                JobState(
                    jobId = jobId,
                    status = JobStatus.CREATED,
                )
            ) { acc, _ ->
                acc.apply {
                    addPage(
                        page = PageState(
                            status = PageStatus.CREATED,
                            data = PageData(
                                itemIds = List(
                                    uuidsPerPage.coerceAtLeast(0),
                                ) { UUID.randomUUID() },
                            ),
                        )
                    )
                }
            }.let {
                jobStateRepository.save(it)
            }.also {
                logger().info(
                    "Generated {} page(s) for job {} with {} UUID(s) per page",
                    numberOfPages,
                    jobId,
                    uuidsPerPage,
                )
            }
}

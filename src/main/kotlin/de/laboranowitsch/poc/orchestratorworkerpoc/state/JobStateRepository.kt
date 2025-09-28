package de.laboranowitsch.poc.orchestratorworkerpoc.state

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobStateRepository : JpaRepository<JobState, UUID> {
    fun findByJobId(jobId: String): JobState?
}

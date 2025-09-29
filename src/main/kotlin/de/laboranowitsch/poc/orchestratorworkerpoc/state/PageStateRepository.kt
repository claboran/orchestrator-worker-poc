package de.laboranowitsch.poc.orchestratorworkerpoc.state

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PageStateRepository : JpaRepository<PageState, UUID> {
    fun findByJobStateJobId(jobId: String): List<PageState>
}


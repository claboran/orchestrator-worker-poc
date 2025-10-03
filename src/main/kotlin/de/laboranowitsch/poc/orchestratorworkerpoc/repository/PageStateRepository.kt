package de.laboranowitsch.poc.orchestratorworkerpoc.repository

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PageStateRepository : JpaRepository<PageState, UUID> {
    fun findByJobStateId(id: UUID): List<PageState>
}

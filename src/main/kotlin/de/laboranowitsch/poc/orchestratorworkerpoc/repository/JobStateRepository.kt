package de.laboranowitsch.poc.orchestratorworkerpoc.repository

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.JobState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobStateRepository : JpaRepository<JobState, UUID>

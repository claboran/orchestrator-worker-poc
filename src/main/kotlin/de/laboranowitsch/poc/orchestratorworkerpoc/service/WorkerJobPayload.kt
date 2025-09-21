package de.laboranowitsch.poc.orchestratorworkerpoc.service

import java.time.Instant

/**
 * Payload for worker job messages containing task information.
 */
data class WorkerJobPayload(
    val jobId: String,
    val taskId: String,
    val data: String,
    val taskNumber: Int,
    val totalTasks: Int,
    val createdAt: Instant = Instant.now()
)

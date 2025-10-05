package de.laboranowitsch.poc.orchestratorworkerpoc.data

import de.laboranowitsch.poc.orchestratorworkerpoc.entity.PageData

data class WorkerJobPayload(
    val jobId: String,
    val pageId: String,
    val data: PageData,
)

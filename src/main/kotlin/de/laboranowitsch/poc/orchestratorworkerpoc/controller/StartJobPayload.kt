package de.laboranowitsch.poc.orchestratorworkerpoc.controller

import java.io.Serializable

/**
 * Payload for starting a new job through the control queue.
 */
data class StartJobPayload(
    val someData: String,
    val priority: String = "NORMAL",
    val description: String? = null,
)

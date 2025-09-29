package de.laboranowitsch.poc.orchestratorworkerpoc.controller

data class StartJobPayload(
    val someData: String,
    val priority: String = "NORMAL",
    val description: String? = null,
)

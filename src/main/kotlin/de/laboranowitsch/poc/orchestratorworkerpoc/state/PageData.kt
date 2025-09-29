package de.laboranowitsch.poc.orchestratorworkerpoc.state

import java.util.UUID

data class PageData(
    val itemIds: List<UUID> = emptyList(),
)


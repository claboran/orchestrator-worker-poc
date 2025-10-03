package de.laboranowitsch.poc.orchestratorworkerpoc.entity

import java.util.UUID

data class PageData(
    val itemIds: List<UUID> = emptyList(),
)


package de.laboranowitsch.poc.orchestratorworkerpoc.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = StartJobMessage::class, name = "START_JOB"),
    JsonSubTypes.Type(value = PageDoneMessage::class, name = "PAGE_DONE"),
)
sealed interface OrchestratorMessage

data class StartJobMessage(
    val jobId: String,
) : OrchestratorMessage

data class PageDoneMessage(
    val jobId: String,
    val pageId: String,
    val pageStatus: PageStatus,
    val errorMessage: String? = null,
) : OrchestratorMessage


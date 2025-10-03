package de.laboranowitsch.poc.orchestratorworkerpoc.data

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Sealed interface representing all possible messages that can be received by the JobOrchestrator.
 * Jackson polymorphic deserialization is configured via annotations.
 */
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

/**
 * Message to start a new job orchestration.
 */
data class StartJobMessage(
    val someData: String,
    val description: String? = null,
    val priority: String? = null,
) : OrchestratorMessage

/**
 * Message indicating a page processing task has completed.
 */
data class PageDoneMessage(
    val pageId: String,
    val success: Boolean,
    val errorMessage: String? = null,
) : OrchestratorMessage


package de.laboranowitsch.poc.orchestratorworkerpoc.data

import java.time.Instant

/**
 * DTO for error handling when sending error messages back to control queue.
 */
data class ErrorJobPayload(
    val originalJobId: String,
    val errorMessage: String,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val failedAt: Instant = Instant.now(),
)



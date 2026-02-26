package org.example.dto
import java.util.UUID

/**
 * DTO for returning response by endpoint /executions.
 */
data class CreateExecutionResponse(
    val id: UUID,
    val status: String,
)
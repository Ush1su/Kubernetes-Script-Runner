package org.example.dto

import java.util.UUID

/**
 * DTO for returning execution status by endpoint /executions/{id}.
 */
data class ExecutionStatusDTO (
    val id: UUID,
    val status: String,
    val createdAt: String,
    val outcome: String? = null
)
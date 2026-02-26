package org.example.dto

/**
 * DTO for representing the state of a Kubernetes Job.
 * Contains the status (QUEUED, IN_PROGRESS, FINISHED) and outcome (SUCCEEDED, FAILED, null if not finished)
 */
data class JobState(
    val status: ExecutionStatus,
    val outcome: String? = null
)
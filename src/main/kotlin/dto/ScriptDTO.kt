package org.example.dto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * DTO for specifying execution resources.
 */
data class ExecResourcesDTO(
    @field:Pattern(
        regexp = "^(\\d+m|\\d+(\\.\\d+)?)$",
        message = "cpu must be like '500m' or '1' or '1.5'"
    )
    val cpu: String = "500m",

    @field:Pattern(
        regexp = "^\\d+(Mi|Gi)$",
        message = "memory must be like '256Mi' or '1Gi'"
    )
    val memory: String = "256Mi"
)

/**
 * DTO for input data for execution creation.
 */
data class ScriptDTO(
    @field:NotBlank(message = "script must not be blank")
    val script: String,

    @field:Valid
    val resources: ExecResourcesDTO = ExecResourcesDTO()
)

/**
 * Enum representing the status of an execution.
 */
enum class ExecutionStatus { QUEUED, IN_PROGRESS, FINISHED }
package org.example.entities
import org.example.dto.ExecutionStatus
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

/**
 * Entity class representing an execution.
 */
@Entity
@Table(name = "executions")
class Execution(

    @field:Id
    @field:GeneratedValue
    @field:UuidGenerator
    @field:Column(columnDefinition = "uuid")
    var id: UUID? = null,

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    var status: ExecutionStatus = ExecutionStatus.QUEUED,

    @field:Column(nullable = false, columnDefinition = "text")
    var script: String,

    @field:Column(name = "requested_cpu")
    var requestedCpu: String? = null,

    @field:Column(name = "requested_memory_mb")
    var requestedMemoryMb: String? = null,

    @field:Column(name = "job_name")
    var jobName: String? = null,

    @field:Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @field:Column(name = "outcome")
    var outcome: String? = null,
) {
    constructor() : this(script = "")
}
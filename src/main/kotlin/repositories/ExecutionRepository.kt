package org.example.repositories

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import org.example.dto.ExecutionStatus
import org.example.entities.Execution

/**
 * DB Repository for managing executions.
 */
interface ExecutionRepository : JpaRepository<Execution, UUID> {
    fun findByStatusNotAndJobNameIsNotNull(status: ExecutionStatus): List<Execution>
    fun findByJobName(jobName: String): Execution?
}

package org.example.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.entities.Execution
import org.example.dto.ExecutionStatus
import org.example.dto.JobState
import org.example.dto.ScriptDTO
import org.example.repositories.ExecutionRepository
import java.time.Instant
import java.util.UUID
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

/**
 * Service for managing executions in the database.
 */
@Service
class ExecutionDBService(private val repo: ExecutionRepository) {

    /**
     * Creates a new execution and saves it to the database.
     */
    @Transactional
    fun createExecution(script: ScriptDTO, jobName: String): UUID {
        val entity = Execution(
            id = null,
            status = ExecutionStatus.QUEUED,
            script = script.script,
            jobName = jobName,
            requestedCpu = script.resources.cpu,
            requestedMemoryMb = script.resources.memory,
            createdAt = Instant.now()
        )
        return repo.save(entity).id!!
    }

    /**
     * Gets all active executions.
     */
    @Transactional(readOnly = true)
    fun getActiveExecutions(): List<Execution> {
        return repo.findByStatusNotAndJobNameIsNotNull(ExecutionStatus.FINISHED)
    }

    /**
     * Updates the status of the execution with the given ID.
     */
    @Transactional
    fun updateStatuses(statuses: Map<UUID, ExecutionStatus>) {
        if (statuses.isEmpty()) return
        val executions = repo.findAllById(statuses.keys)

        for (execution in executions) {
            val newStatus = statuses[execution.id]
            if (newStatus != null) {
                execution.status = newStatus
            }
        }
    }

    /**
     * Updates the outcome of the execution with the given ID.
     */
    @Transactional
    fun updateOutcomes(outcomes: Map<UUID, String?>) {
        if (outcomes.isEmpty()) return
        val executions = repo.findAllById(outcomes.keys)
        for (execution in executions) {
            val newOutcome = outcomes[execution.id]
            if (newOutcome != null) {
                execution.outcome = newOutcome
            }
        }
    }

    /**
     * Updates the status and outcome of an execution with the given job name.
     */
    @Transactional
    fun updateStatusByJobName(jobName: String, jobState: JobState) {
        val execution = repo.findByJobName(jobName) ?: return
        execution.status = jobState.status
        execution.outcome = jobState.outcome
    }

    /**
     * Gets an execution by its ID.
     */
    fun getExecution(id: UUID): Execution {
        return repo.findById(id)
            .orElseThrow {
                ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Execution $id not found"
                )
            }
    }
}
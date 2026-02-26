package org.example.services

import org.example.dto.CreateExecutionResponse
import org.example.dto.ExecutionStatus
import org.example.dto.ExecutionStatusDTO
import org.example.dto.ScriptDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Service for managing executions (combines Kubernetes and database services)
 */
@Service
class ExecutorService (
    private val kubernetesService: KubernetesExecutorService,
    private val dbService: ExecutionDBService,
    @Value("\${EXEC_QUEUE_TIMEOUT_SECONDS:300}") private val queueTimeoutSeconds: Long,
) {
    /**
     * Updates execution statuses and outcomes based on Kubernetes job states.
     * Runs every 3 seconds to update job statuses for the user in real time.
     */
    @Scheduled(fixedDelay = 3000)
    fun reconcileUnfinishedExecutions() {
        val executions = dbService.getActiveExecutions()
        if (executions.isEmpty()) return

        val jobNames = executions.mapNotNull { it.jobName }
        if (jobNames.isEmpty()) return

        val nameToState = try {
            kubernetesService.getStatesForJobs(jobNames)
        } catch (e: Exception) {
            println("Failed to fetch Kubernetes job states: ${e.message}")
            return
        }

        val statusUpdates = mutableMapOf<UUID, ExecutionStatus>()
        val outcomeUpdates = mutableMapOf<UUID, String>()
        val now = Instant.now()

        for (execution in executions) {
            val jobName = execution.jobName ?: continue
            val state = nameToState[jobName] ?: continue
            val createdAt = execution.createdAt
            if (execution.status != ExecutionStatus.FINISHED) {
                val queuedTooLong = execution.status == ExecutionStatus.QUEUED &&
                        Duration.between(createdAt, now).seconds >= queueTimeoutSeconds
                if (queuedTooLong) {
                    statusUpdates[execution.id!!] = ExecutionStatus.FINISHED
                    outcomeUpdates[execution.id!!] = "START_TIMEOUT: job did not start within ${queueTimeoutSeconds}s"
                    continue
                }
            }
            val oldStatus = execution.status
            val newStatus = state.status

            if (newStatus != oldStatus) {
                statusUpdates[execution.id!!] = newStatus
            }

            val becameFinished = oldStatus != ExecutionStatus.FINISHED && newStatus == ExecutionStatus.FINISHED

            if (becameFinished && state.outcome != null) {
                outcomeUpdates[execution.id!!] = state.outcome
            }
        }

        if (statusUpdates.isNotEmpty()) {
            dbService.updateStatuses(statusUpdates)
        }
        if (outcomeUpdates.isNotEmpty()) {
            dbService.updateOutcomes(outcomeUpdates)
        }
    }

    /**
     * Executes a script and returns the execution ID.
     */
    fun execute(script: ScriptDTO): CreateExecutionResponse {
        val jobName = "exec-" + UUID.randomUUID().toString().take(8)
        val id = dbService.createExecution(script, jobName)
        kubernetesService.createShellJobAsync(jobName, script)
        return CreateExecutionResponse(id, ExecutionStatus.QUEUED.name)
    }

    /**
     * Gets the status of an execution by its ID.
     */
    fun getExecution(id: UUID) : ExecutionStatusDTO{
        val entity = dbService.getExecution(id)
        return ExecutionStatusDTO(id, entity.status.name, entity.createdAt.toString(), entity.outcome)
    }
}
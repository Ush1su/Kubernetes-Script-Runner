package org.example.services

import org.example.dto.ScriptDTO
import org.example.dto.ExecutionStatus
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.models.*
import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.util.ClientBuilder
import io.kubernetes.client.util.KubeConfig
import org.example.dto.JobState
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileReader

/**
Creates and monitors Kubernetes Jobs for shell script execution.
*/
@Service
class KubernetesExecutorService(
    @Value("\${K8S_NAMESPACE:default}") private val namespace: String,
    private val dbService: ExecutionDBService,
    kubeConfigPath: String? = null
) {
    private val apiClient: ApiClient = buildClient(kubeConfigPath)
    private val batchApi: BatchV1Api = BatchV1Api(apiClient)

    /**
     * Creates a Job asynchronously.
     * On failure, marks execution as FINISHED with error message.
     */
    @Async
    fun createShellJobAsync(jobName: String, script: ScriptDTO, image: String = "alpine:3.20") {
        try {
            createShellJob(jobName, script, image)
        } catch (e: Exception) {
            dbService.updateStatusByJobName(
                jobName,
                JobState(status = ExecutionStatus.FINISHED, outcome = e.message)
            )
        }
    }

    /**
     * Builds and submits a Kubernetes Job for the given script.
     */
    fun createShellJob(
        jobName: String,
        script: ScriptDTO,
        image: String = "alpine:3.20"
    ) {
        val labels = mapOf(
            "app" to "executor-api",
            "jobName" to jobName,
        )

        val resources = V1ResourceRequirements()
            .putRequestsItem("cpu", Quantity.fromString(script.resources.cpu))
            .putRequestsItem("memory", Quantity.fromString(script.resources.memory))
            .putLimitsItem("cpu", Quantity.fromString(script.resources.cpu))
            .putLimitsItem("memory", Quantity.fromString(script.resources.memory))

        val container = V1Container()
            .name("runner")
            .image(image)
            .command(listOf("/bin/sh", "-c", script.script))
            .resources(resources)

        val podSpec = V1PodSpec()
            .restartPolicy("Never")
            .containers(listOf(container))

        val template = V1PodTemplateSpec()
            .metadata(V1ObjectMeta().labels(labels))
            .spec(podSpec)

        val job = V1Job()
            .apiVersion("batch/v1")
            .kind("Job")
            .metadata(V1ObjectMeta().name(jobName).labels(labels))
            .spec(
                V1JobSpec()
                    .template(template)
                    .backoffLimit(0)
                    .activeDeadlineSeconds(720)
                    .ttlSecondsAfterFinished(300)
            )

        batchApi.createNamespacedJob(namespace, job).execute()
    }

    /**
     * Retrieves Job states from Kubernetes API to use them in status updates in DB.
     */
    fun getStatesForJobs(jobNames: List<String>): Map<String, JobState> {
        val jobs = batchApi.listNamespacedJob(namespace)
            .labelSelector("app=executor-api")
            .execute()
            .items

        val byName = jobs.associateBy { it.metadata?.name }

        return jobNames.mapNotNull { name ->
            val job = byName[name] ?: return@mapNotNull null
            val st = job.status
            val active = st?.active ?: 0
            val succeeded = st?.succeeded ?: 0
            val failed = st?.failed ?: 0

            val status = when {
                active > 0 -> ExecutionStatus.IN_PROGRESS
                succeeded > 0 || failed > 0 -> ExecutionStatus.FINISHED
                else -> ExecutionStatus.QUEUED
            }

            val outcome = if (status == ExecutionStatus.FINISHED) {
                when {
                    succeeded > 0 -> "SUCCESS"
                    failed > 0 -> "FAILED"
                    else -> "FAILED"
                }
            } else null

            name to JobState(status, outcome)
        }.toMap()
    }

    /**
     * Build Client that interacts with Kubernetes API.
     */
    private fun buildClient(kubeConfigPath: String? = null): ApiClient {
        val saTokenFile = File("/var/run/secrets/kubernetes.io/serviceaccount/token")
        val inCluster = System.getenv("KUBERNETES_SERVICE_HOST") != null || saTokenFile.exists()

        val client = if (inCluster) {
            ClientBuilder.cluster().build()
        } else {
            val path = kubeConfigPath
                ?: System.getenv("KUBECONFIG")
                ?: "${System.getProperty("user.home")}/.kube/config"

            val kubeConfig = KubeConfig.loadKubeConfig(FileReader(path))
            ClientBuilder.kubeconfig(kubeConfig).build()
        }

        Configuration.setDefaultApiClient(client)
        return client
    }
}
package org.example.controllers

import jakarta.validation.Valid
import org.example.dto.CreateExecutionResponse
import org.example.dto.ExecutionStatusDTO
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.example.dto.ScriptDTO
import org.example.services.ExecutorService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@RestController
@RequestMapping("/executions")
class ExecutionController (
    private val executorService: ExecutorService,
) {
    @GetMapping("/health")
    fun hello_world() = "API is up and running!"

    @PostMapping
    fun executeScript(@Valid @RequestBody dto: ScriptDTO) : CreateExecutionResponse {
        val response = executorService.execute(dto)
        return response
    }
    @GetMapping("/{id}")
    fun getExecutionStatus(@PathVariable id: UUID): ExecutionStatusDTO {
        return executorService.getExecution(id)
    }
}
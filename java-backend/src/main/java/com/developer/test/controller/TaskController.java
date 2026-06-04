package com.developer.test.controller;

import com.developer.test.dto.CreateTaskRequest;
import com.developer.test.dto.UpdateTaskRequest;
import com.developer.test.dto.TasksResponse;
import com.developer.test.model.Task;
import com.developer.test.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * Handles task API requests for listing, creating, and updating tasks.
 * It keeps the HTTP layer simple and passes the real work to the service layer.
 * That separation makes the task flow easier to test and easier to explain.
 */
@Tag(name = "Tasks", description = "List, create, and update task records")
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    
    private final TaskService taskService;
    
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
    
    @GetMapping
    @Operation(summary = "List tasks", description = "Returns tasks and supports optional status and user filters.")
    public ResponseEntity<TasksResponse> getTasks(
            @Parameter(description = "Optional task status filter")
            @RequestParam(required = false) String status,
            @Parameter(description = "Optional user id filter")
            @RequestParam(required = false) String userId) {
        List<Task> tasks = taskService.getTasks(status, userId);
        TasksResponse response = new TasksResponse(tasks, tasks.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create task", description = "Creates a task after validating the request body.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest request) {
        // Create returns 201 and the stored task so the API behavior is clear and consistent.
        Task createdTask = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Updates an existing task by id.")
    @SecurityRequirement(name = "apiKeyAuth")
    public ResponseEntity<Task> updateTask(@Parameter(description = "Task id") @PathVariable int id,
                                           @Valid @RequestBody UpdateTaskRequest request) {
        Task updatedTask = taskService.updateTask(id, request);
        return ResponseEntity.ok(updatedTask);
    }
}

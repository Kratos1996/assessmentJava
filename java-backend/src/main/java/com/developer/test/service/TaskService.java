package com.developer.test.service;

import com.developer.test.dto.CreateTaskRequest;
import com.developer.test.dto.UpdateTaskRequest;
import com.developer.test.exception.BadRequestException;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.Task;
import com.developer.test.model.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Owns the task rules for validation, lookup, creation, and update.
 * It also checks user references before anything is written to the datastore.
 * That keeps task handling predictable and protects the shared in-memory state.
 */
@Service
public class TaskService {
    private final DataStore dataStore;
    private final StatsService statsService;

    public TaskService(DataStore dataStore, StatsService statsService) {
        this.dataStore = dataStore;
        this.statsService = statsService;
    }

    public List<Task> getTasks(String status, String userId) {
        // We validate query parameters here so bad filters come back as client errors, not server errors.
        if (status != null && StringUtils.hasText(status) && !TaskStatus.isValid(status.trim())) {
            throw new BadRequestException("status must be pending, in-progress, or completed");
        }

        if (userId != null && StringUtils.hasText(userId)) {
            try {
                Integer.parseInt(userId.trim());
            } catch (NumberFormatException ex) {
                throw new BadRequestException("userId must be a positive integer");
            }
        }

        return dataStore.getTasks(status, userId);
    }

    public Task getTaskById(int id) {
        Task task = dataStore.getTaskById(id);
        if (task == null) {
            throw new NotFoundException("Task with id " + id + " was not found");
        }
        return task;
    }

    public Task createTask(CreateTaskRequest request) {
        // We validate everything before saving so only clean task data reaches the store.
        String title = normalize(request.getTitle());
        String status = normalize(request.getStatus());
        Integer userId = request.getUserId();

        validateRequired(title, "title");
        validateRequired(status, "status");
        validateRequired(userId, "userId");
        validateStatus(status);
        validateUserExists(userId);

        Task createdTask = dataStore.createTask(title, status, userId);
        statsService.invalidate();
        return createdTask;
    }

    public Task updateTask(int id, UpdateTaskRequest request) {
        Task existing = getTaskById(id);

        String title = request.getTitle() == null ? null : request.getTitle().trim();
        String status = request.getStatus() == null ? null : request.getStatus().trim();
        Integer userId = request.getUserId();

        if (title != null && !StringUtils.hasText(title)) {
            throw new BadRequestException("title cannot be blank");
        }
        if (status != null) {
            if (!StringUtils.hasText(status)) {
                throw new BadRequestException("status cannot be blank");
            }
            validateStatus(status);
        }
        if (userId != null) {
            validateUserExists(userId);
        }

        Task updatedTask = dataStore.updateTask(existing.getId(), title, status, userId);
        statsService.invalidate();
        return updatedTask;
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(fieldName + " is required");
        }
    }

    private void validateRequired(Integer value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required");
        }
    }

    private void validateStatus(String status) {
        if (!TaskStatus.isValid(status)) {
            throw new BadRequestException("status must be pending, in-progress, or completed");
        }
    }

    private void validateUserExists(Integer userId) {
        if (!dataStore.userExists(userId)) {
            throw new BadRequestException("userId " + userId + " does not exist");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}

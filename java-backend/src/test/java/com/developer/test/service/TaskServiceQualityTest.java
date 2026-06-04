package com.developer.test.service;

import com.developer.test.dto.CreateTaskRequest;
import com.developer.test.dto.UpdateTaskRequest;
import com.developer.test.exception.BadRequestException;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for task validation, partial updates, and cache invalidation.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceQualityTest {
    @Mock
    private DataStore dataStore;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskRejectsUnknownUser() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Ship release");
        request.setStatus("pending");
        request.setUserId(99);

        when(dataStore.userExists(99)).thenReturn(false);

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("userId 99 does not exist");
    }

    @Test
    void updateTaskAppliesPartialChangesAndInvalidatesStatsCache() {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Refine onboarding flow");

        when(dataStore.getTaskById(5)).thenReturn(new Task(5, "Original title", "pending", 1));
        when(dataStore.updateTask(5, "Refine onboarding flow", null, null))
                .thenReturn(new Task(5, "Refine onboarding flow", "pending", 1));

        Task updated = taskService.updateTask(5, request);

        assertThat(updated.getTitle()).isEqualTo("Refine onboarding flow");
        verify(dataStore).updateTask(5, "Refine onboarding flow", null, null);
        verify(statsService).invalidate();
    }

    @Test
    void getTaskByIdThrowsNotFoundForMissingTask() {
        when(dataStore.getTaskById(77)).thenReturn(null);

        assertThatThrownBy(() -> taskService.getTaskById(77))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Task with id 77 was not found");
    }
}

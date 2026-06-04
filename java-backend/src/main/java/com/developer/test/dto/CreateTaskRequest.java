package com.developer.test.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Holds the data needed to create a new task.
 * It keeps the request shape small and the validation rules close to the fields.
 * The service uses it to create tasks only after the input is checked.
 */
public class CreateTaskRequest {
    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "status is required")
    @Pattern(regexp = "pending|in-progress|completed", message = "status must be pending, in-progress, or completed")
    private String status;

    @NotNull(message = "userId is required")
    @Min(value = 1, message = "userId must be a positive integer")
    private Integer userId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}

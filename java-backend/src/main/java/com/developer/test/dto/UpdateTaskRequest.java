package com.developer.test.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * Holds the optional fields used when updating a task.
 * Each field can be sent on its own, so the service updates only what changed.
 * This keeps partial updates simple and predictable.
 */
public class UpdateTaskRequest {
    private String title;

    @Pattern(regexp = "pending|in-progress|completed", message = "status must be pending, in-progress, or completed")
    private String status;

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

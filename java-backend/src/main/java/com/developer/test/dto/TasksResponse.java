package com.developer.test.dto;

import com.developer.test.model.Task;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Wraps the list of tasks in the response shape expected by the frontend.
 * The count field gives the caller an easy summary of how many tasks were returned.
 * This keeps the response useful without making the controller more complex.
 */
public class TasksResponse {
    @JsonProperty("tasks")
    private List<Task> tasks;
    
    @JsonProperty("count")
    private int count;

    public TasksResponse() {
    }

    public TasksResponse(List<Task> tasks, int count) {
        this.tasks = tasks;
        this.count = count;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

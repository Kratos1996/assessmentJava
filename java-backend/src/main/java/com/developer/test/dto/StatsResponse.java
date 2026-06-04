package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Holds the summary numbers returned by the stats endpoint.
 * It groups user totals and task totals into one response object.
 * The service fills this object so the controller can return it directly.
 */
public class StatsResponse {
    @JsonProperty("users")
    private UsersStats users;
    
    @JsonProperty("tasks")
    private TasksStats tasks;

    public StatsResponse() {
        this.users = new UsersStats();
        this.tasks = new TasksStats();
    }

    public UsersStats getUsers() {
        return users;
    }

    public void setUsers(UsersStats users) {
        this.users = users;
    }

    public TasksStats getTasks() {
        return tasks;
    }

    public void setTasks(TasksStats tasks) {
        this.tasks = tasks;
    }

    /**
     * Holds the total number of users in the system.
     * The stats endpoint uses it to give a quick high-level view of user data.
     * Keeping it separate makes the response easier to extend later.
     */
    public static class UsersStats {
        @JsonProperty("total")
        private int total;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }

    /**
     * Holds the task counts split by status.
     * The frontend can use it to show the task mix at a glance.
     * This keeps the stats response small and simple to read.
     */
    public static class TasksStats {
        @JsonProperty("total")
        private int total;
        
        @JsonProperty("pending")
        private int pending;
        
        @JsonProperty("inProgress")
        private int inProgress;
        
        @JsonProperty("completed")
        private int completed;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getPending() {
            return pending;
        }

        public void setPending(int pending) {
            this.pending = pending;
        }

        public int getInProgress() {
            return inProgress;
        }

        public void setInProgress(int inProgress) {
            this.inProgress = inProgress;
        }

        public int getCompleted() {
            return completed;
        }

        public void setCompleted(int completed) {
            this.completed = completed;
        }
    }
}

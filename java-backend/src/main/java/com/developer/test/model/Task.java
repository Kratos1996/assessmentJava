package com.developer.test.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Represents one task in the application.
 * It stores the task text, status, and the user that owns it.
 * The API sends and receives this model as the main task payload.
 */
@Entity
@Table(name = "tasks")
public class Task {
    @JsonProperty("id")
    @Id
    private int id;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("userId")
    @Column(name = "user_id")
    private int userId;

    public Task() {
    }

    public Task(int id, String title, String status, int userId) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}

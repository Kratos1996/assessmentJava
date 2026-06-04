package com.developer.test.dto;

import com.developer.test.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Wraps the list of users in the response shape expected by the frontend.
 * It also includes a count so the UI can show totals without recalculating them.
 * Using a response object keeps the API stable if we add more metadata later.
 */
public class UsersResponse {
    @JsonProperty("users")
    private List<User> users;
    
    @JsonProperty("count")
    private int count;

    public UsersResponse() {
    }

    public UsersResponse(List<User> users, int count) {
        this.users = users;
        this.count = count;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

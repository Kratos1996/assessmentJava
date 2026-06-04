package com.developer.test.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

/**
 * Holds the data needed to create a new user.
 * Validation annotations make sure the API gets the name, email, and role it needs.
 * The controller passes this object straight to the service layer.
 */
public class CreateUserRequest {
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "role is required")
    private String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

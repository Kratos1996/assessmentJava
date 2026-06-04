package com.developer.test.controller;

import com.developer.test.dto.CreateUserRequest;
import com.developer.test.dto.UsersResponse;
import com.developer.test.model.User;
import com.developer.test.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import javax.validation.Valid;
import java.util.List;

/**
 * Handles user API requests for listing, reading, and creating users.
 * The controller only translates HTTP traffic into service calls and response objects.
 * All validation and business rules live lower in the stack so the endpoint stays small.
 */
@Tag(name = "Users", description = "Create and read user records")
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    @Operation(summary = "List users", description = "Returns all users with a count wrapper.")
    public ResponseEntity<UsersResponse> getUsers() {
        List<User> users = userService.getUsers();
        UsersResponse response = new UsersResponse(users, users.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by id", description = "Returns one user record by its numeric id.")
    public ResponseEntity<User> getUserById(@Parameter(description = "User id") @PathVariable int id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a user after validating the request body.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // The service handles validation, ID generation, and storage so this endpoint stays easy to follow.
        User createdUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
}

package com.developer.test.service;

import com.developer.test.dto.CreateUserRequest;
import com.developer.test.exception.BadRequestException;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Owns the user rules for this application.
 * It trims input, checks required fields, and creates users through the datastore.
 * Keeping that logic here makes the controller thin and the behavior easy to review.
 */
@Service
public class UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final DataStore dataStore;
    private final StatsService statsService;

    public UserService(DataStore dataStore, StatsService statsService) {
        this.dataStore = dataStore;
        this.statsService = statsService;
    }

    public List<User> getUsers() {
        return dataStore.getUsers();
    }

    public User getUserById(int id) {
        User user = dataStore.getUserById(id);
        if (user == null) {
            throw new NotFoundException("User with id " + id + " was not found");
        }
        return user;
    }

    public User createUser(CreateUserRequest request) {
        // The service owns the business rules so the controller stays small and easy to read.
        String name = normalize(request.getName());
        String email = normalize(request.getEmail());
        String role = normalize(request.getRole());

        validateRequired(name, "name");
        validateRequired(email, "email");
        validateRequired(role, "role");

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("email must be a valid email address");
        }

        User createdUser = dataStore.createUser(name, email, role);
        statsService.invalidate();
        return createdUser;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(fieldName + " is required");
        }
    }
}

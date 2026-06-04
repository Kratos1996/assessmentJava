package com.developer.test.service;

import com.developer.test.dto.AuthResponse;
import com.developer.test.dto.LoginRequest;
import com.developer.test.dto.RegisterRequest;
import com.developer.test.exception.BadRequestException;
import com.developer.test.exception.NotFoundException;
import com.developer.test.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final DataStore dataStore;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(DataStore dataStore, PasswordService passwordService, JwtService jwtService) {
        this.dataStore = dataStore;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String name = normalize(request.getName());
        String email = normalize(request.getEmail());
        String role = normalize(request.getRole());
        String password = normalize(request.getPassword());

        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("name is required");
        }
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("email is required");
        }
        if (!StringUtils.hasText(role)) {
            throw new BadRequestException("role is required");
        }
        if (!StringUtils.hasText(password)) {
            throw new BadRequestException("password is required");
        }

        if (dataStore.getUserByEmail(email) != null) {
            throw new BadRequestException("email already exists");
        }

        String salt = passwordService.generateSalt();
        String passwordHash = passwordService.hashPassword(password, salt);
        User user = dataStore.createUser(name, email, role, passwordHash, salt);
        return createResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.getEmail());
        String password = normalize(request.getPassword());

        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new BadRequestException("email and password are required");
        }

        User user = dataStore.getUserByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) || !StringUtils.hasText(user.getPasswordSalt())) {
            throw new BadRequestException("User is not configured for password login");
        }
        if (!passwordService.matches(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return createResponse(user);
    }

    private AuthResponse createResponse(User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(jwtService.createToken(user));
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtService.getTtlSeconds());
        response.setUser(user);
        return response;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}

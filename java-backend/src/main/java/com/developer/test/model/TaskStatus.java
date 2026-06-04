package com.developer.test.model;

import java.util.Arrays;

/**
 * Defines the allowed task status values in one place.
 * Keeping the list here helps validation stay consistent across the app.
 * Services and DTOs both use it to avoid magic strings spreading through the code.
 */
public enum TaskStatus {
    PENDING("pending"),
    IN_PROGRESS("in-progress"),
    COMPLETED("completed");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String candidate) {
        return candidate != null && Arrays.stream(values())
                .anyMatch(status -> status.value.equals(candidate));
    }
}

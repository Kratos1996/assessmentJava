package com.developer.test.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error envelope returned by the API.
 * It gives clients a status code,  message, the request path, and optional field details.
 * Keeping the format fixed makes errors easier to handle on the frontend and easier to review in tests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("error")
    private final String error;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("path")
    private final String path;

    @JsonProperty("details")
    private final List<FieldErrorDetail> details;

    public ApiErrorResponse(int status, String error, String message, String path, List<FieldErrorDetail> details) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }

    /**
     * Describes one field-level validation problem.
     * The API can return several of these when multiple inputs need to be fixed.
     * That helps the client show clear form errors back to the user.
     */
    public static class FieldErrorDetail {
        @JsonProperty("field")
        private final String field;

        @JsonProperty("message")
        private final String message;

        public FieldErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}

package com.developer.test.filter;

import com.developer.test.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Writes API errors as JSON for the filter layer.
 * That lets early failures use the same response shape as controller exceptions.
 * Keeping it in one place avoids duplicate error-handling code across filters.
 */
@Component
public class ApiErrorResponseWriter {
    private final ObjectMapper objectMapper;

    public ApiErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request,
                      HttpServletResponse response,
                      int status,
                      String error,
                      String message) throws IOException {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                status,
                error,
                message,
                request.getRequestURI(),
                Collections.emptyList()
        );
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

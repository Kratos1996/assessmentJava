package com.developer.test.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects write requests that do not send JSON bodies.
 * We fail fast here so controllers only see requests in the format they expect.
 * That keeps the API responses consistent and the error handling easy to follow.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class JsonRequestValidationFilter extends OncePerRequestFilter {
    private final ApiErrorResponseWriter errorResponseWriter;

    public JsonRequestValidationFilter(ApiErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (requiresJsonBody(request) && !isJsonRequest(request)) {
            errorResponseWriter.write(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    
                    "Bad Request",
                    "Content-Type must be application/json for write operations"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresJsonBody(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return ("POST".equals(method) || "PUT".equals(method))
                && ("/api/users".equals(path) || path.startsWith("/api/tasks"));
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("application/json");
    }
}

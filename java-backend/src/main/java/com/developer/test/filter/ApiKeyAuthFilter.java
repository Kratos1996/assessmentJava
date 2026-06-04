package com.developer.test.filter;

import com.developer.test.service.JwtService;
import com.developer.test.service.MetricsService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 35)
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    private final ApiErrorResponseWriter errorResponseWriter;
    private final MetricsService metricsService;
    private final JwtService jwtService;

    public ApiKeyAuthFilter(ApiErrorResponseWriter errorResponseWriter,
                            MetricsService metricsService,
                            JwtService jwtService) {
        this.errorResponseWriter = errorResponseWriter;
        this.metricsService = metricsService;
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedWriteRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            metricsService.recordAuthFailure();
            errorResponseWriter.write(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "A valid Bearer token is required"
            );
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (jwtService.parseToken(token) == null) {
            metricsService.recordAuthFailure();
            errorResponseWriter.write(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "The supplied JWT is invalid or expired"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedWriteRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))
                && path.startsWith("/api/")
                && !path.startsWith("/api/auth/")
                && !"/api/metrics".equals(path)
                && !"/api/health".equals(path);
    }
}

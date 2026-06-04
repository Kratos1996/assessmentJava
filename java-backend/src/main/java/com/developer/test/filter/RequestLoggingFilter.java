package com.developer.test.filter;

import com.developer.test.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Adds a request id and logs one line for every incoming request.
 * This makes it easy to trace a call from start to finish when we debug the API.
 * The filter also feeds the metrics service so request activity stays visible.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final MetricsService metricsService;

    public RequestLoggingFilter(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            String query = request.getQueryString();
            String path = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            int status = response.getStatus();

            // One log line per request keeps debugging easy during interviews and in production-style reviews.
            if (status >= 500) {
                log.error("requestId={} method={} path={} status={} durationMs={}",
                        requestId, request.getMethod(), path, status, durationMs);
            } else if (status >= 400) {
                log.warn("requestId={} method={} path={} status={} durationMs={}",
                        requestId, request.getMethod(), path, status, durationMs);
            } else {
                log.info("requestId={} method={} path={} status={} durationMs={}",
                        requestId, request.getMethod(), path, status, durationMs);
            }

            metricsService.recordRequest(request.getMethod(), path, status, durationMs);
            MDC.remove("requestId");
        }
    }
}

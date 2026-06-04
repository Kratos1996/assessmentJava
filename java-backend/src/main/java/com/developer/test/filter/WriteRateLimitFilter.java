package com.developer.test.filter;

import com.developer.test.service.MetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limits bursts of write traffic with a small in-memory counter.
 * The rule is simple on purpose so it is easy to explain and easy to test.
 * It helps protect the demo API without adding a heavier rate-limiting system.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class WriteRateLimitFilter extends OncePerRequestFilter {
    private static final String RATE_LIMIT_TITLE = "Too Many Requests";
    private static final String RATE_LIMIT_DETAIL =
            "Write rate limit exceeded. Try again later.";
    private final ApiErrorResponseWriter errorResponseWriter;
    private final MetricsService metricsService;
    private final int maxWritesPerWindow;
    private final long windowMillis;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    public WriteRateLimitFilter(ApiErrorResponseWriter errorResponseWriter,
                                MetricsService metricsService,
                                @Value("${app.security.rate-limit.write.requests-per-window:5}") int maxWritesPerWindow,
                                @Value("${app.security.rate-limit.window-ms:60000}") long windowMillis) {
        this.errorResponseWriter = errorResponseWriter;
        this.metricsService = metricsService;
        this.maxWritesPerWindow = maxWritesPerWindow;
        this.windowMillis = windowMillis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedWriteRequest(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getHeader("Authorization");
        if (key == null || key.trim().isEmpty()) {
            key = request.getRemoteAddr();
        }

        long now = System.currentTimeMillis();
        WindowState state = windows.computeIfAbsent(key, ignored -> new WindowState(now));

        synchronized (state) {
            if (now - state.windowStartMillis >= windowMillis) {
                state.windowStartMillis = now;
                state.requestCount.set(0);
            }

            if (state.requestCount.incrementAndGet() > maxWritesPerWindow) {
                metricsService.recordRateLimited();
                errorResponseWriter.write(
                        request,
                        response,
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        RATE_LIMIT_TITLE,
                        RATE_LIMIT_DETAIL
                );
                return;
            }
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

    private static class WindowState {
        private long windowStartMillis;
        private final AtomicInteger requestCount = new AtomicInteger();

        private WindowState(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
        }
    }
}

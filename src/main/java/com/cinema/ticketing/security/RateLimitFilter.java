package com.cinema.ticketing.security;

import com.cinema.ticketing.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequests;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties) {
        this.maxRequests = appProperties.getRateLimit().getRequestsPerMinute();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr();
        long minute = Instant.now().getEpochSecond() / 60;

        Counter counter = counters.compute(key, (k, v) -> {
            if (v == null || v.minute != minute) {
                return new Counter(minute, 1);
            }
            v.count++;
            return v;
        });

        if (counter.count > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"message\":\"Rate limit exceeded\"}");
            response.setContentType("application/json");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static class Counter {
        long minute;
        int count;

        Counter(long minute, int count) {
            this.minute = minute;
            this.count = count;
        }
    }
}

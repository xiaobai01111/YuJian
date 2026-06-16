package com.campus.wall.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_MDC_KEY = "traceId";
    private static final int TRACE_ID_MAX_LENGTH = 64;
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_HEADER));

        String previous = MDC.get(TRACE_MDC_KEY);
        MDC.put(TRACE_MDC_KEY, traceId);
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previous != null) {
                MDC.put(TRACE_MDC_KEY, previous);
            } else {
                MDC.remove(TRACE_MDC_KEY);
            }
        }
    }

    private String resolveTraceId(String incomingTraceId) {
        if (!StringUtils.hasText(incomingTraceId)) {
            return generateTraceId();
        }
        String normalized = incomingTraceId.trim();
        if (!isValidTraceId(normalized)) {
            return generateTraceId();
        }
        return normalized;
    }

    private boolean isValidTraceId(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return false;
        }
        if (traceId.length() > TRACE_ID_MAX_LENGTH) {
            return false;
        }
        return TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

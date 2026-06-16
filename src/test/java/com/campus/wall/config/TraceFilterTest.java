package com.campus.wall.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceFilterTest {

    private final TraceFilter traceFilter = new TraceFilter();

    @Test
    void doFilterInternal_validHeader_keepsIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceFilter.TRACE_HEADER, "abcDEF123_-");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceInChain.set(MDC.get(TraceFilter.TRACE_MDC_KEY));

        traceFilter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceFilter.TRACE_HEADER)).isEqualTo("abcDEF123_-");
        assertThat(traceInChain.get()).isEqualTo("abcDEF123_-");
        assertThat(MDC.get(TraceFilter.TRACE_MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_invalidHeader_generatesSafeTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceFilter.TRACE_HEADER, "bad\r\ntrace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceInChain.set(MDC.get(TraceFilter.TRACE_MDC_KEY));

        traceFilter.doFilter(request, response, chain);

        String traceId = response.getHeader(TraceFilter.TRACE_HEADER);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).isEqualTo(traceInChain.get());
        assertThat(traceId).hasSize(32);
        assertThat(traceId).matches("^[a-f0-9]{32}$");
        assertThat(MDC.get(TraceFilter.TRACE_MDC_KEY)).isNull();
    }

    @Test
    void doFilterInternal_overLengthHeader_generatesSafeTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceFilter.TRACE_HEADER, "a".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
        };

        traceFilter.doFilter(request, response, chain);

        String traceId = response.getHeader(TraceFilter.TRACE_HEADER);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).hasSize(32);
        assertThat(traceId).matches("^[a-f0-9]{32}$");
        assertThat(MDC.get(TraceFilter.TRACE_MDC_KEY)).isNull();
    }
}

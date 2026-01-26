package com.campus.wall.property;

import com.campus.wall.config.TraceFilter;
import jakarta.servlet.FilterChain;
import net.jqwik.api.*;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 23: 链路追踪一致性
 * 验证 TraceFilter 会生成/透传 traceId，并写入响应头与 MDC。
 */
class TracePropertyTest {

    private final TraceFilter traceFilter = new TraceFilter();

    @Property(tries = 100)
    void traceIdConsistentAcrossRequestAndResponse(@ForAll("traceIdInputs") Optional<String> incoming) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        if (incoming.isPresent()) {
            request.addHeader(TraceFilter.TRACE_HEADER, incoming.get());
        }

        AtomicReference<String> traceInChain = new AtomicReference<>();
        FilterChain chain = (req, res) -> traceInChain.set(MDC.get(TraceFilter.TRACE_MDC_KEY));

        traceFilter.doFilter(request, response, chain);

        String responseTrace = response.getHeader(TraceFilter.TRACE_HEADER);
        assertThat(responseTrace).isNotBlank();
        assertThat(traceInChain.get()).isEqualTo(responseTrace);

        incoming.ifPresent(expected -> assertThat(responseTrace).isEqualTo(expected));
        assertThat(MDC.get(TraceFilter.TRACE_MDC_KEY)).isNull();
    }

    @Provide
    Arbitrary<Optional<String>> traceIdInputs() {
        Arbitrary<String> traceIds = Arbitraries.strings()
            .withCharRange('a', 'f')
            .withCharRange('0', '9')
            .ofMinLength(8)
            .ofMaxLength(32);
        return Arbitraries.oneOf(
            Arbitraries.just(Optional.empty()),
            traceIds.map(Optional::of)
        );
    }
}

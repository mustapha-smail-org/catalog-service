package com.citypulse.catalog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldPropagateValidCorrelationIdAndClearMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-42:test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> valueInsideChain = new AtomicReference<>();

        filter.doFilter(request, response, captureMdc(valueInsideChain));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-42:test");
        assertThat(valueInsideChain).hasValue("request-42:test");
        assertThat(org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldReplaceInvalidCorrelationIdWithUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "invalid value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void shouldClearMdcWhenChainFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                    throw new ServletException("failure");
                })
        ).isInstanceOf(ServletException.class);

        assertThat(org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private FilterChain captureMdc(AtomicReference<String> value) {
        return (request, response) -> value.set(
                org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY)
        );
    }
}

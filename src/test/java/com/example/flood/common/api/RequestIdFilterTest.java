package com.example.flood.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter(new PublicIdGenerator(
        Clock.fixed(Instant.parse("2026-08-21T00:00:00Z"), ZoneOffset.UTC)));

    @AfterEach
    void clearContext() {
        RequestContextHolder.clear();
    }

    @Test
    void generatesRequestIdAndClearsThreadLocalAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideRequest = new AtomicReference<>();
        FilterChain chain = (req, res) ->
            insideRequest.set(RequestContextHolder.requireCurrent().requestId());

        filter.doFilter(request, response, chain);

        assertThat(insideRequest.get()).startsWith("req_");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(insideRequest.get());
        assertThat(RequestContextHolder.current()).isEmpty();
    }

    @Test
    void preservesValidClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(RequestIdFilter.HEADER_NAME, "client.request:42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideRequest = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) ->
            insideRequest.set(RequestContextHolder.requireCurrent().requestId()));

        assertThat(insideRequest.get()).isEqualTo("client.request:42");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("client.request:42");
    }

    @Test
    void replacesInvalidClientRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.addHeader(RequestIdFilter.HEADER_NAME, "contains spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).startsWith("req_");
    }
}

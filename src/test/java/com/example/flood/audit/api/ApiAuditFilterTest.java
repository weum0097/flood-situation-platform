package com.example.flood.audit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.flood.audit.application.ApiAuditRecord;
import com.example.flood.audit.application.ApiAuditService;
import com.example.flood.common.api.RequestContext;
import com.example.flood.common.api.RequestContextHolder;
import com.example.flood.common.idempotency.CanonicalRequestHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAuditFilterTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach void clear() { RequestContextHolder.clear(); }

    @Test
    void recordsMetadataAndCommonErrorCodeWithoutApiKey() throws Exception {
        ApiAuditService service = mock(ApiAuditService.class);
        ApiAuditFilter filter = new ApiAuditFilter(service, objectMapper,
            new CanonicalRequestHasher(objectMapper));
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RequestContextHolder.set(new RequestContext("req_1", 7L, 8L, "client", Set.of(), "127.0.0.1"));

        filter.doFilter(request, response, (req, res) -> {
            req.getInputStream().readAllBytes();
            var http = (jakarta.servlet.http.HttpServletResponse) res;
            http.setStatus(422);
            http.getWriter().write("{\"errorCode\":\"NO_SITUATION_DATA\"}");
        });

        ArgumentCaptor<ApiAuditRecord> record = ArgumentCaptor.forClass(ApiAuditRecord.class);
        verify(service).record(record.capture());
        assertThat(record.getValue().errorCode()).isEqualTo("NO_SITUATION_DATA");
        assertThat(record.getValue().clientId()).isEqualTo(7);
        assertThat(record.getValue().requestHash()).hasSize(32);
        assertThat(response.getContentAsString()).contains("NO_SITUATION_DATA");
    }

    @Test
    void auditFailureDoesNotChangeBusinessResponse() throws Exception {
        ApiAuditService service = mock(ApiAuditService.class);
        doThrow(new IllegalStateException("audit down")).when(service).record(any());
        ApiAuditFilter filter = new ApiAuditFilter(service, objectMapper,
            new CanonicalRequestHasher(objectMapper));
        RequestContextHolder.set(new RequestContext("req_2", null, null, null, Set.of(), "127.0.0.1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request(), response, (req, res) ->
            ((jakarta.servlet.http.HttpServletResponse) res).getWriter().write("ok"));

        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST",
            "/openapi/v1/material-demand-calculations");
        request.addHeader("X-API-Key", "prefix.complete-secret-that-must-not-be-stored");
        request.setContentType("application/json");
        request.setContent("{\"b\":2,\"a\":1}".getBytes(StandardCharsets.UTF_8));
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}

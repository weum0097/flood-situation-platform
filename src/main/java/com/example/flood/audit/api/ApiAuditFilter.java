package com.example.flood.audit.api;

import com.example.flood.audit.application.ApiAuditRecord;
import com.example.flood.audit.application.ApiAuditService;
import com.example.flood.common.api.RequestContext;
import com.example.flood.common.api.RequestContextHolder;
import com.example.flood.common.idempotency.CanonicalRequestHasher;
import com.example.flood.common.idempotency.IdempotentOperation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@ConditionalOnBean(ApiAuditService.class)
public class ApiAuditFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAuditFilter.class);
    private final ApiAuditService service;
    private final ObjectMapper objectMapper;
    private final CanonicalRequestHasher hasher;

    public ApiAuditFilter(ApiAuditService service, ObjectMapper objectMapper,
        CanonicalRequestHasher hasher) {
        this.service = service; this.objectMapper = objectMapper; this.hasher = hasher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/openapi/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        long started = System.nanoTime();
        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } finally {
            try {
                RequestContext context = RequestContextHolder.requireCurrent();
                byte[] body = cachedRequest.getContentAsByteArray();
                service.record(new ApiAuditRecord(context.requestId(), context.clientId(),
                    context.apiKeyId(), request.getMethod(), request.getRequestURI(),
                    cachedResponse.getStatus(), errorCode(cachedResponse.getContentAsByteArray()),
                    Math.max(0, (System.nanoTime() - started) / 1_000_000), context.remoteIp(),
                    body.length == 0 ? null : hash(request, body)));
            } catch (Exception auditFailure) {
                LOGGER.error("API audit persistence failed", auditFailure);
            } finally {
                cachedResponse.copyBodyToResponse();
            }
        }
    }

    private byte[] hash(HttpServletRequest request, byte[] body) throws IOException {
        JsonNode json = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
        return hasher.hash(new IdempotentOperation(
            request.getMethod() + ":" + request.getRequestURI(), Map.of(), Map.of()), json);
    }

    private String errorCode(byte[] body) {
        if (body.length == 0) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode code = root.get("errorCode");
            return code == null || !code.isTextual() ? null : code.textValue();
        } catch (Exception ignored) {
            return null;
        }
    }
}

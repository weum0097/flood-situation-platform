package com.example.flood.security.api;

import com.example.flood.common.api.ApiErrorResponse;
import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.RequestContext;
import com.example.flood.common.api.RequestContextHolder;
import com.example.flood.security.application.ApiKeyAuthenticationService;
import com.example.flood.security.application.ApiPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnBean(ApiKeyAuthenticationService.class)
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    public static final String API_KEY_HEADER = "X-API-Key";
    private final ApiKeyAuthenticationService authenticationService;
    private final ScopeAuthorizationManager authorizationManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiKeyAuthenticationFilter(ApiKeyAuthenticationService authenticationService,
        ScopeAuthorizationManager authorizationManager, ObjectMapper objectMapper, Clock clock) {
        this.authenticationService = authenticationService;
        this.authorizationManager = authorizationManager;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/openapi/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        try {
            ApiPrincipal principal = authenticationService.authenticate(
                request.getHeader(API_KEY_HEADER), request.getRemoteAddr());
            authorizationManager.authorize(principal, request.getMethod(), request.getRequestURI());
            var authorities = principal.scopes().stream().map(SimpleGrantedAuthority::new).toList();
            SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
            RequestContext old = RequestContextHolder.requireCurrent();
            RequestContextHolder.set(new RequestContext(old.requestId(), principal.clientId(),
                principal.apiKeyId(), principal.clientCode(), principal.scopes(), old.remoteIp()));
            chain.doFilter(request, response);
        } catch (ApiException exception) {
            writeError(response, exception);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void writeError(HttpServletResponse response, ApiException exception) throws IOException {
        response.setStatus(exception.errorCode().status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
            RequestContextHolder.requireCurrent().requestId(), exception.errorCode().name(),
            exception.getMessage(), List.of(), OffsetDateTime.now(clock));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package com.example.flood.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private final PublicIdGenerator idGenerator;

    public RequestIdFilter(PublicIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String candidate = request.getHeader(HEADER_NAME);
        String requestId = candidate != null && VALID_REQUEST_ID.matcher(candidate).matches()
            ? candidate
            : idGenerator.next("req_");
        response.setHeader(HEADER_NAME, requestId);
        RequestContextHolder.set(new RequestContext(
            requestId, null, null, null, Set.of(), request.getRemoteAddr()));
        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContextHolder.clear();
        }
    }
}

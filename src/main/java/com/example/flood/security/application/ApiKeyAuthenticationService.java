package com.example.flood.security.application;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.security.infrastructure.ApiKeyMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ApiKeyMapper.class)
@ConditionalOnProperty(name = "flood.persistence.enabled", havingValue = "true", matchIfMissing = true)
public class ApiKeyAuthenticationService {

    private static final Pattern KEY_PATTERN = Pattern.compile(
        "^flood_live_([A-Za-z0-9]{8,32})\\.([A-Za-z0-9_-]{32,})$");
    private final ApiKeyMapper mapper;
    private final ApiKeyHasher hasher;
    private final ClientRateLimiter rateLimiter;
    private final Clock clock;

    public ApiKeyAuthenticationService(
        ApiKeyMapper mapper,
        ApiKeyHasher hasher,
        ClientRateLimiter rateLimiter,
        Clock clock
    ) {
        this.mapper = mapper;
        this.hasher = hasher;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public ApiPrincipal authenticate(String rawKey, String remoteIp) {
        Matcher matcher = rawKey == null ? null : KEY_PATTERN.matcher(rawKey);
        if (matcher == null || !matcher.matches()) {
            throw invalidKey();
        }
        ApiKeyCredential credential = mapper.findCredentialByPrefix(matcher.group(1))
            .orElseThrow(ApiKeyAuthenticationService::invalidKey);
        Instant now = clock.instant();
        if (!"ACTIVE".equals(credential.keyStatus())
            || !"ACTIVE".equals(credential.clientStatus())
            || !credential.expiresAt().isAfter(now)
            || !hasher.matches(rawKey, credential.secretHash())
            || (!credential.allowedIps().isEmpty() && !credential.allowedIps().contains(remoteIp))) {
            throw invalidKey();
        }
        if (!rateLimiter.tryAcquire(
            credential.clientId(), credential.rateLimitPerMinute(), now)) {
            throw new ApiException(ErrorCode.RATE_LIMIT_EXCEEDED, "API rate limit exceeded");
        }
        mapper.touchLastUsed(credential.apiKeyId(), now.minus(5, ChronoUnit.MINUTES), now);
        return new ApiPrincipal(
            credential.clientId(), credential.apiKeyId(), credential.clientCode(), credential.scopes());
    }

    private static ApiException invalidKey() {
        return new ApiException(ErrorCode.INVALID_API_KEY, "API key is invalid or expired");
    }
}

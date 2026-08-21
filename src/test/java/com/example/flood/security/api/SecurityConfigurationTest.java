package com.example.flood.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.security.application.ApiKeyAuthenticationService;
import com.example.flood.security.application.ApiKeyCredential;
import com.example.flood.security.application.ApiKeyHasher;
import com.example.flood.security.application.ApiPrincipal;
import com.example.flood.security.application.ClientRateLimiter;
import com.example.flood.security.infrastructure.ApiKeyMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecurityConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");
    private static final String RAW_KEY =
        "flood_live_abcdefgh.abcdefghijklmnopqrstuvwxyz012345";

    @Test
    void rejectsExpiredAndRevokedCredentialsWithoutLeakingRawKey() {
        assertInvalid(credential("ACTIVE", "ACTIVE", NOW.minusSeconds(1)));
        assertInvalid(credential("REVOKED", "ACTIVE", NOW.plusSeconds(60)));
    }

    @Test
    void authenticatesValidCredentialAndEnforcesScope() {
        ApiKeyCredential credential = credential("ACTIVE", "ACTIVE", NOW.plusSeconds(60));
        ApiKeyMapper mapper = mock(ApiKeyMapper.class);
        when(mapper.findCredentialByPrefix("abcdefgh")).thenReturn(Optional.of(credential));
        ApiKeyAuthenticationService service = service(mapper);

        ApiPrincipal principal = service.authenticate(RAW_KEY, "127.0.0.1");
        assertThat(principal.clientCode()).isEqualTo("test-client");

        ScopeAuthorizationManager scopes = new ScopeAuthorizationManager();
        assertThatThrownBy(() -> scopes.authorize(
                principal, "POST", "/openapi/v1/material-demand-calculations"))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.errorCode())
                    .isEqualTo(ErrorCode.INSUFFICIENT_SCOPE));
    }

    @Test
    void operationToScopeMappingIsExplicit() {
        ScopeAuthorizationManager scopes = new ScopeAuthorizationManager();

        assertThat(scopes.requiredScope("POST", "/openapi/v1/disaster-events"))
            .contains("event:write");
        assertThat(scopes.requiredScope("GET", "/openapi/v1/disaster-events"))
            .contains("event:read");
        assertThat(scopes.requiredScope(
            "POST", "/openapi/v1/material-demand-calculations/from-region-data"))
            .contains("material:calculate");
    }

    private static void assertInvalid(ApiKeyCredential credential) {
        ApiKeyMapper mapper = mock(ApiKeyMapper.class);
        when(mapper.findCredentialByPrefix("abcdefgh")).thenReturn(Optional.of(credential));

        assertThatThrownBy(() -> service(mapper).authenticate(RAW_KEY, "127.0.0.1"))
            .isInstanceOfSatisfying(ApiException.class, exception -> {
                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_API_KEY);
                assertThat(exception.getMessage()).doesNotContain(RAW_KEY);
            });
    }

    private static ApiKeyAuthenticationService service(ApiKeyMapper mapper) {
        return new ApiKeyAuthenticationService(
            mapper,
            new ApiKeyHasher("unit-test-pepper"),
            new ClientRateLimiter(),
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ApiKeyCredential credential(
        String keyStatus,
        String clientStatus,
        Instant expiresAt
    ) {
        ApiKeyHasher hasher = new ApiKeyHasher("unit-test-pepper");
        return new ApiKeyCredential(
            10L, 20L, "test-client", "abcdefgh", hasher.hash(RAW_KEY),
            Set.of("event:write"), keyStatus, clientStatus, expiresAt, null,
            60, List.of("127.0.0.1"));
    }
}

package com.example.flood.security.application;

import com.example.flood.security.infrastructure.ApiClientMapper;
import com.example.flood.security.infrastructure.ApiKeyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "flood.security.bootstrap", name = "enabled", havingValue = "true")
public class BootstrapApiKeyInitializer implements ApplicationRunner {
    private static final Pattern KEY_PATTERN = Pattern.compile(
        "^flood_live_([A-Za-z0-9]{8,32})\\.([A-Za-z0-9_-]{32,})$");
    private static final Set<String> ALL_SCOPES = Set.of(
        "event:write", "event:read", "situation:calculate", "situation:read", "material:calculate");
    private final SecurityProperties properties;
    private final ApiClientMapper clientMapper;
    private final ApiKeyMapper keyMapper;
    private final ApiKeyHasher hasher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BootstrapApiKeyInitializer(SecurityProperties properties, ApiClientMapper clientMapper,
        ApiKeyMapper keyMapper, ApiKeyHasher hasher, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.clientMapper = clientMapper;
        this.keyMapper = keyMapper;
        this.hasher = hasher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        String code = properties.bootstrap().clientCode();
        String rawKey = properties.bootstrap().apiKey();
        if (code == null || code.isBlank() || rawKey == null) {
            throw new IllegalStateException("Bootstrap client code and API key are required when enabled");
        }
        Matcher matcher = KEY_PATTERN.matcher(rawKey);
        if (!matcher.matches()) throw new IllegalStateException("Bootstrap API key format is invalid");
        clientMapper.upsert(code);
        long clientId = clientMapper.findIdByCode(code).orElseThrow();
        keyMapper.upsert(clientId, matcher.group(1), hasher.hash(rawKey),
            objectMapper.writeValueAsString(ALL_SCOPES),
            clock.instant().plus(properties.bootstrapKeyTtl()));
    }
}

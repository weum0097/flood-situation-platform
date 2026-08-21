package com.example.flood.security.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("flood.security")
public record SecurityProperties(
    String apiKeyPepper,
    Bootstrap bootstrap,
    Duration bootstrapKeyTtl
) {
    public SecurityProperties {
        bootstrap = bootstrap == null ? new Bootstrap(false, null, null) : bootstrap;
        bootstrapKeyTtl = bootstrapKeyTtl == null ? Duration.ofDays(365) : bootstrapKeyTtl;
    }

    public record Bootstrap(boolean enabled, String clientCode, String apiKey) {}
}

package com.example.flood.common.idempotency;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("flood.idempotency")
public record IdempotencyProperties(Duration ttl) {
    public IdempotencyProperties {
        ttl = ttl == null ? Duration.ofHours(24) : ttl;
    }
}

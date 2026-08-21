package com.example.flood.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdempotencyConfiguration {
    @Bean
    CanonicalRequestHasher canonicalRequestHasher(ObjectMapper objectMapper) {
        return new CanonicalRequestHasher(objectMapper);
    }
}

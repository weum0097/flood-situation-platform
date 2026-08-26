package com.example.flood.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class IdempotencyConfiguration {
    @Bean
    CanonicalRequestHasher canonicalRequestHasher(ObjectMapper objectMapper) {
        return new CanonicalRequestHasher(objectMapper);
    }
}

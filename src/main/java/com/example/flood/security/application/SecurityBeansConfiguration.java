package com.example.flood.security.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityBeansConfiguration {
    @Bean
    ApiKeyHasher apiKeyHasher(SecurityProperties properties) {
        return new ApiKeyHasher(properties.apiKeyPepper());
    }

    @Bean
    ClientRateLimiter clientRateLimiter() {
        return new ClientRateLimiter();
    }
}

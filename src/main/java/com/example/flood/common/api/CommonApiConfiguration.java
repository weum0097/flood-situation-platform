package com.example.flood.common.api;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CommonApiConfiguration {

    @Bean
    PublicIdGenerator publicIdGenerator(Clock clock) {
        return new PublicIdGenerator(clock);
    }
}

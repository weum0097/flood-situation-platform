package com.example.flood.situation.application;

import com.example.flood.situation.domain.SituationRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SituationDomainConfiguration {
    @Bean
    SituationRuleEngine situationRuleEngine() {
        return new SituationRuleEngine();
    }
}

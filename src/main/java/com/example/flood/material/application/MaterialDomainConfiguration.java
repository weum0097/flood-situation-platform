package com.example.flood.material.application;

import com.example.flood.material.domain.MaterialDemandCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MaterialDomainConfiguration {
    @Bean
    MaterialDemandCalculator materialDemandCalculator() { return new MaterialDemandCalculator(); }
}

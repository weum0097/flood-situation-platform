package com.example.flood;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LocalProfileConfigurationTest {

    @Test
    void localProfileUsesPortThatDoesNotConflictWithDockerDesktop() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
                .profiles("local")
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run()) {
            assertThat(context.getEnvironment().getProperty("server.port", Integer.class))
                    .isEqualTo(18080);
            assertThat(context.getEnvironment().getProperty("spring.jackson.time-zone"))
                    .isEqualTo("Asia/Shanghai");
            assertThat(context.getEnvironment().getProperty(
                    "spring.jackson.deserialization.adjust-dates-to-context-time-zone",
                    Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("logging.pattern.dateformat"))
                    .contains("Asia/Shanghai");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
    }
}

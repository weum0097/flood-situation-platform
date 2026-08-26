package com.example.flood;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = FloodApplication.class,
    properties = {
    "spring.flyway.enabled=false",
    "flood.persistence.enabled=false",
        "flood.security.bootstrap.enabled=false",
        "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
            + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
    }
)
class ApplicationContextTest {
    @Autowired ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void requestDeserializerPreservesCallerOffsetForBeijingValidation() throws Exception {
        TimeRequest request = objectMapper.readValue(
            "{\"time\":\"2026-08-21T00:00:00Z\"}", TimeRequest.class);

        assertThat(request.time().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    private record TimeRequest(OffsetDateTime time) {}
}

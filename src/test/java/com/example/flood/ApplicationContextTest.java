package com.example.flood;

import org.junit.jupiter.api.Test;
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

    @Test
    void contextLoads() {
    }
}

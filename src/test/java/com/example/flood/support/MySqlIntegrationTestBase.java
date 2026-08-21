package com.example.flood.support;

import com.example.flood.FloodApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@ActiveProfiles("test")
@SpringBootTest(classes = FloodApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class MySqlIntegrationTestBase {
    protected static final String API_KEY =
        "flood_live_testpref.0123456789abcdefghijklmnopqrstuvwxyzABCDEFG";

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("flood_scenario_deduction")
        .withUsername("root").withPassword("root");

    static { MYSQL.start(); }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.locations",
            () -> "classpath:sql/mysql,classpath:sql/mysql-local");
        registry.add("flood.security.api-key-pepper", () -> "integration-test-pepper");
        registry.add("flood.security.bootstrap.enabled", () -> true);
        registry.add("flood.security.bootstrap.client-code", () -> "integration-client");
        registry.add("flood.security.bootstrap.api-key", () -> API_KEY);
    }

    @Autowired protected TestRestTemplate rest;
    @Autowired protected JdbcTemplate jdbc;

    protected HttpEntity<String> json(String body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", API_KEY);
        if (idempotencyKey != null) headers.set("Idempotency-Key", idempotencyKey);
        return new HttpEntity<>(body, headers);
    }
}

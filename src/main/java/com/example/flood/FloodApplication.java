package com.example.flood;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FloodApplication {

    public static void main(String[] args) {
        SpringApplication.run(FloodApplication.class, args);
    }
}

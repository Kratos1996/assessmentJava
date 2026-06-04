package com.developer.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Spring Boot application for the Java backend.
 * The real application behavior lives in controllers, services, filters, and the datastore.
 * This class stays small so the entry point is easy to read and easy to trust.
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(Application.class, args);
    }
}

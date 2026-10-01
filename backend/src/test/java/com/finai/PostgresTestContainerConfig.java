package com.finai;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The PostgreSQL container used by the integration suites.
 *
 * Declaring the container as a static {@code @Bean} lets Spring Boot start it and
 * contribute the connection details, so the JUnit Testcontainers extension is not
 * needed and a single container is shared by every integration test class.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    @ServiceConnection
    @Bean
    static PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("finai")
                .withUsername("finai")
                .withPassword("finai");
    }
}

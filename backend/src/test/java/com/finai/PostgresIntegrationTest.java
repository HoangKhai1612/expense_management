package com.finai;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Shared wiring for the Testcontainers suites.
 *
 * The backend is integration tested against a real PostgreSQL instance rather than
 * an in-memory substitute: the schema, the Flyway migrations and the aggregation
 * queries are the parts most likely to break silently, and only a real engine
 * exercises them.
 */
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
public abstract class PostgresIntegrationTest {

    @DynamicPropertySource
    static void runMigrationsForReal(DynamicPropertyRegistry registry) {
        // The migrations are part of what is under test, so Flyway must run for real.
        registry.add("spring.flyway.enabled", () -> "true");
    }
}

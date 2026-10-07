package com.example.subscription;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots the application against an ephemeral PostgreSQL
 * container, so every test run starts from a clean database and exercises the same SQL dialect
 * that production uses. The container is shared across the whole test JVM (static field +
 * static initialiser), which keeps the suite fast.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("subscription_service_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // The application starts its own Postgres via docker-compose for local dev. In tests
        // we already have a container, so the built-in one must not start and steal the port.
        registry.add("spring.docker.compose.enabled", () -> "false");
        // JobRunr has its own background job server; tests need it on the classpath but
        // should not fight over recurring jobs from a parallel test JVM.
        registry.add("org.jobrunr.background-job-server.enabled", () -> "false");
        registry.add("org.jobrunr.dashboard.enabled", () -> "false");
    }
}
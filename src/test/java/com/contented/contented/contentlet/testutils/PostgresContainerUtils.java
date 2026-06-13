package com.contented.contented.contentlet.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresContainerUtils {

    public static PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("demo")
            .withUsername("contented")
            .withPassword("example");
    }

    public static void startAndRegisterPostgresContainer(PostgreSQLContainer postgresContainer, DynamicPropertyRegistry registry) {
        postgresContainer.start();
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}

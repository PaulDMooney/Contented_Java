package com.contented.contented;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.postgresContainer;
import static com.contented.contented.contentitem.testutils.PostgresContainerUtils.startAndRegisterPostgresContainer;
import static com.contented.contented.contentitem.testutils.TestTypeTags.INTEGRATION_TESTS;

// Needs a database at startup: Spring Data JDBC resolves its dialect over a connection and
// Liquibase runs the changelog, so the context can no longer load without one.
@Tag(INTEGRATION_TESTS)
@SpringBootTest
@Testcontainers
@DisplayName("ContentedApplication")
class ContentedApplicationIT {

	@Container
	static PostgreSQLContainer postgres = postgresContainer();

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		startAndRegisterPostgresContainer(postgres, registry);
	}

	@Test
	@DisplayName("It should load the application context")
	void contextLoads() {
	}

}

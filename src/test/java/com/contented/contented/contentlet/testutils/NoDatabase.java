package com.contented.contented.contentlet.testutils;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For a {@code @SpringBootTest} that doesn't touch the database: excludes the datasource and
 * Liquibase auto-configuration and disables the app's {@code JdbcConfig}, so no Postgres container
 * is needed. The rest of the JDBC stack (JdbcTemplate, transaction manager, Spring Data JDBC
 * repositories) is conditional on a {@code DataSource}, so it backs off on its own.
 *
 * The full application context still loads, so mock any DB-backed bean it wires in — typically
 * {@code @MockitoBean ContentletRepository}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
@TestPropertySource(properties = "contented.persistence.jdbc.enabled=false")
public @interface NoDatabase {
}

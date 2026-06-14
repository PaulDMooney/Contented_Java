package com.contented.contented.contentlet.testutils;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * For a {@code @SpringBootTest} that doesn't touch the database: excludes the JDBC/Liquibase
 * auto-configuration and the app's {@code JdbcConfig}, so no Postgres container is needed.
 *
 * The full application context still loads, so mock any DB-backed bean it wires in — typically
 * {@code @MockitoBean ContentletRepository}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration," +
        "org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration," +
        "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration," +
        "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration",
    "contented.persistence.jdbc.enabled=false"
})
public @interface NoDatabase {
}

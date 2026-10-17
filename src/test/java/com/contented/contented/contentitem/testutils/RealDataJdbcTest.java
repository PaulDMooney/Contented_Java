package com.contented.contented.contentitem.testutils;

import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@code @DataJdbcTest} against the real configured database (e.g. a Testcontainers container
 * registered via {@code @DynamicPropertySource}) rather than an embedded replacement.
 *
 * Also auto-configures Jackson: the slice picks up the app's {@code JdbcConfig}, whose jsonb
 * converters need an {@code ObjectMapper}, so the context can't load without it.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureJson
public @interface RealDataJdbcTest {
}

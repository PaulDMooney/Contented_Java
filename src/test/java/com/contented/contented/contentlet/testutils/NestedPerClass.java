package com.contented.contented.contentlet.testutils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.TYPE})
@Nested
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface NestedPerClass {
}

package com.contented.contented;

import org.junit.jupiter.api.*;

@DisplayName("Nested Discovery Test")
public class NestedDiscoveryTest {

    @BeforeAll()
    static void beforeAll() {
        System.out.println("A. Before All");
    }

    @BeforeEach
    void beforeEach() {
        System.out.println("A. Before Each test method");
    }

    @AfterEach
    void afterEach() {
        System.out.println("A. After each test method");
    }

    @AfterAll()
    static void afterAll() {
        System.out.println("A. After All");
    }

    @Test
    @DisplayName("A. First Test")
    void firstTest() {
        System.out.println("A. First Test Done");
    }

    @Nested
    @DisplayName("Specific Method 1")
    class SpecificMethod1 {

        @BeforeAll()
        static void beforeAll() {
            System.out.println("B. Before All");
        }

        @BeforeEach
        void beforeEach() {
            System.out.println("B. Before Each test method");
        }

        @AfterEach
        void afterEach() {
            System.out.println("B. After each test method");
        }

        @AfterAll()
        static void afterAll() {
            System.out.println("B. After All");
        }

        @Test
        @DisplayName("B. Specific Method 1")
        void firstTest() {
            System.out.println("B. First Test Done");
        }

        @Nested
        @DisplayName("Specific Use case 1")
        class EvenDeeper {
            @Test
            @DisplayName("C. Specific Method 1")
            void firstTest() {
                System.out.println("C. First Test Done");
            }
        }

    }


}

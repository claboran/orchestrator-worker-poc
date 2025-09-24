@file:Suppress("unused")
package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base test class for database-only integration tests.
 * Use this when you only need PostgreSQL and don't require SQS functionality.
 * 
 * Usage:
 * - Extend this class for integration tests that only need database access
 * - Use @SpringBootTest annotation on your test class as usual
 * - This provides a lighter alternative to BaseIntegrationTest when SQS is not needed
 */
@Testcontainers
abstract class BaseDatabaseTest {
    companion object {
        // PostgreSQL container with standard test database setup
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // Configure PostgreSQL properties
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}
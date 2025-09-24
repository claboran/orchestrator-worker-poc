package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.annotation.AnnotationRetention
import kotlin.annotation.AnnotationTarget

/**
 * Custom annotation for database integration tests using PostgreSQL with @ServiceConnection.
 * This replaces the need to extend BaseDatabaseTest and provides automatic configuration
 * of PostgreSQL properties through Spring Boot's service connection feature.
 * 
 * Usage:
 * ```kotlin
 * @DatabaseTest
 * class MyRepositoryTest {
 *     // Test methods here - PostgreSQL will be automatically configured
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@Testcontainers
annotation class DatabaseTest

/**
 * Container holder for database tests with @ServiceConnection support.
 */
abstract class DatabaseTestContainer {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
}
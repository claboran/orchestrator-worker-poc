package de.laboranowitsch.poc.orchestratorworkerpoc.testutil

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(
    TestContainerConfig::class,
    DynamicTestContainersPropertyConfig::class,
)
annotation class IntegrationTests

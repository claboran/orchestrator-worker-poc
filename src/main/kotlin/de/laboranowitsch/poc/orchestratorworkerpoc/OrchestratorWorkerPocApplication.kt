package de.laboranowitsch.poc.orchestratorworkerpoc

import de.laboranowitsch.poc.orchestratorworkerpoc.util.cli.ApplicationMode
import de.laboranowitsch.poc.orchestratorworkerpoc.util.cli.CommandLineParser
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OrchestratorWorkerPocApplication : LoggingAware

fun main(args: Array<String>) {
    val app = OrchestratorWorkerPocApplication()
    val parser = CommandLineParser()
    val mode = parser.parse(args)

    runApplication<OrchestratorWorkerPocApplication>(*args) {
        when (mode) {
            ApplicationMode.ORCHESTRATOR -> {
                app.logger().info("Starting in ORCHESTRATOR mode...")
                setAdditionalProfiles("orchestrator")
            }
            ApplicationMode.WORKER -> {
                app.logger().info("Starting in WORKER mode...")
                setAdditionalProfiles("worker")
            }
        }
    }
}

package de.laboranowitsch.poc.orchestratorworkerpoc.util.cli

import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import kotlin.system.exitProcess

enum class ApplicationMode {
    ORCHESTRATOR,
    WORKER,
}

class CommandLineParser : LoggingAware {
    private val options = Options().apply {
        addOption(
            "m",
            "mode",
            true,
            "Application run mode: orchestrator, or worker",
        )
    }

    fun parse(args: Array<String>): ApplicationMode =
        runCatching {
            DefaultParser().parse(options, args)
        }.fold(
            onSuccess = { cmd ->
                when (val mode = cmd.getOptionValue("mode")?.uppercase()) {
                    "ORCHESTRATOR" -> ApplicationMode.ORCHESTRATOR
                    "WORKER" -> ApplicationMode.WORKER
                    else -> {
                        val msg = if (mode == null) "Error: --mode is required." else "Error: Invalid mode '$mode'."
                        logger().error("$msg Allowed values: ORCHESTRATOR, WORKER.")
                        exitProcess(1)
                    }
                }
            },
            onFailure = { e ->
                logger().error("Error parsing command line arguments: ${e.message}")
                exitProcess(1)
            }
        )
}

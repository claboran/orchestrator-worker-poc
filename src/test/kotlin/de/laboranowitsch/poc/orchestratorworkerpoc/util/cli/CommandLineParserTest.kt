package de.laboranowitsch.poc.orchestratorworkerpoc.util.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CommandLineParserTest {

    @Test
    fun `parse returns ORCHESTRATOR for --mode orchestrator`() {
        val parser = CommandLineParser()

        val result = parser.parse(arrayOf("--mode", "orchestrator"))

        assertEquals(ApplicationMode.ORCHESTRATOR, result)
    }

    @Test
    fun `parse returns WORKER for -m worker`() {
        val parser = CommandLineParser()

        val result = parser.parse(arrayOf("-m", "worker"))

        assertEquals(ApplicationMode.WORKER, result)
    }
}

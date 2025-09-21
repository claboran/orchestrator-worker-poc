package de.laboranowitsch.poc.orchestratorworkerpoc.util.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Interface providing logging functionality to classes.
 */
interface LoggingAware

/**
 * Extension function to get a logger for any LoggingAware class.
 */
fun LoggingAware.logger(): Logger = LoggerFactory.getLogger(this::class.java)

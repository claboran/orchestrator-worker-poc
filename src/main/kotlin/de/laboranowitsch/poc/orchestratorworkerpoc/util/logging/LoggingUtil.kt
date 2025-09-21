package de.laboranowitsch.poc.orchestratorworkerpoc.util.logging

import org.slf4j.LoggerFactory

interface LoggingAware

inline fun <reified T : LoggingAware> T.logger() = LoggerFactory.getLogger(T::class.java)

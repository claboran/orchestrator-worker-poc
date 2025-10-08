package de.laboranowitsch.poc.orchestratorworkerpoc.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.LoggingAware
import de.laboranowitsch.poc.orchestratorworkerpoc.util.logging.logger
import io.awspring.cloud.sqs.config.SqsListenerConfigurer
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
class SqsCustomConfig : LoggingAware {
    /**
     * Defines the ObjectMapper specifically for SQS message serialization/deserialization.
     * This ObjectMapper is used by both the SqsTemplate (for sending) over SqsMessageSender
     * and SqsListenerConfigurer (for receiving) to ensure consistent
     * serialization/deserialization of our polymorphic sealed interfaces.
     */
    @Bean(SQS_OBJECT_MAPPER)
    fun sqsObjectMapper(): ObjectMapper = ObjectMapper().apply {
        // This is essential for proper serialization/deserialization of Kotlin data classes
        registerKotlinModule()
        // Add other ObjectMapper customizations here if needed (e.g., date formats)
    }.also {
        logger().info("Created dedicated SQS ObjectMapper with Kotlin module")
    }

    /**
     * Configures the message converter for SQS listeners (receiving messages).
     * This ensures incoming messages are properly deserialized from JSON.
     */
    @Bean
    fun sqsListenerConfigurer(
        @Qualifier(SQS_OBJECT_MAPPER) objectMapper: ObjectMapper,
    ): SqsListenerConfigurer = SqsListenerConfigurer { registry ->
        registry.apply {
            setObjectMapper(objectMapper)
        }
    }.also {
        logger().info("Configured SqsListenerConfigurer with custom ObjectMapper")
    }

    /**
     * Provides a MappingJackson2MessageConverter for SQS message serialization/deserialization.
     * This converter is explicitly used by the SqsTemplate bean to ensure proper JSON handling
     * of our polymorphic sealed interfaces.
     */
    @Bean(SQS_MESSAGE_CONVERTER)
    fun sqsMessageConverter(
        @Qualifier(SQS_OBJECT_MAPPER) objectMapper: ObjectMapper,
    ): MappingJackson2MessageConverter = MappingJackson2MessageConverter().apply {
        setObjectMapper(objectMapper)
        setSerializedPayloadClass(String::class.java)
        isStrictContentTypeMatch = false
    }.also {
        logger().info("Configured MappingJackson2MessageConverter for SQS with custom ObjectMapper")
    }

    /**
     * Factory bean for creating SQS message listener containers.
     */
    @Bean(SQS_MESSAGE_CONTAINER_FACTORY)
    fun sqsMessageContainerFactory(
        asyncClient: SqsAsyncClient,
    ): SqsMessageListenerContainerFactory<Any> = SqsMessageListenerContainerFactory
        .builder<Any>()
        .sqsAsyncClient(asyncClient)
        .configure { options ->
            options.autoStartup(true)
            options.maxConcurrentMessages(1)
            options.maxMessagesPerPoll(1)
        }.build()
        .also {
            logger().info("Created SqsMessageListenerContainerFactory with injected message converter")
        }

    companion object {
        const val SQS_OBJECT_MAPPER = "sqsObjectMapper"
        const val SQS_MESSAGE_CONVERTER = "sqsMessageConverter"
        const val SQS_MESSAGE_CONTAINER_FACTORY = "sqsMessageContainerFactory"
    }
}

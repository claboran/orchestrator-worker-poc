package de.laboranowitsch.poc.orchestratorworkerpoc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.net.URI

@Configuration
class AwsSqsConfiguration(
    @param:Value("\${spring.cloud.aws.region.static:}")
    private val region: String,
    @param:Value("\${spring.cloud.aws.credentials.access-key:}")
    private val accessKey: String,
    @param:Value("\${spring.cloud.aws.credentials.secret-key:}")
    private val secretKey: String,
    @param:Value("\${spring.cloud.aws.sqs.endpoint:}")
    private val endpoint: String,
) {

    @Bean
    @Primary
    @ConditionalOnMissingBean(SqsAsyncClient::class)
    fun sqsAsyncClient(): SqsAsyncClient {
        val builder = SqsAsyncClient.builder()

        if (region.isNotBlank()) {
            builder.region(Region.of(region))
        }

        if (accessKey.isNotBlank() && secretKey.isNotBlank()) {
            val creds = AwsBasicCredentials.create(accessKey, secretKey)
            builder.credentialsProvider(StaticCredentialsProvider.create(creds))
        }

        if (endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(endpoint))
        }

        return builder.build()
    }
}


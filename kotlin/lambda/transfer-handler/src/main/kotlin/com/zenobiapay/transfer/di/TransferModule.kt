package com.zenobiapay.transfer.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.METRIC_NAMESPACE
import com.zenobiapay.table.model.HmacSecret
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import jakarta.inject.Named
import software.amazon.awssdk.services.sqs.SqsClient

const val PAGINATION_SECRET = "PAGINATION_SECRET"
const val TRANSFER_NOTIFICATION_SECRET = "TRANSFER_NOTIFICATION_SECRET"
const val AVAILABLE_BALANCE_BUFFER = "AVAILABLE_BALANCE_BUFFER"
const val TRANSFER_METADATA_QUEUE_URL = "TRANSFER_METADATA_QUEUE_URL"

private val logger = KotlinLogging.logger {}

@Module
class TransferModule {
    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    @Named(PAGINATION_SECRET)
    fun providePaginationSecret(secretsManagerClient: SecretsManagerClient): String {
        return secretsManagerClient.getSecretValue {
            it.secretId("pagination/hmac")
        }.secretString()
    }

    @Provides
    @Named(TRANSFER_NOTIFICATION_SECRET)
    fun provideTransferNotificationSecret(objectMapper: ObjectMapper, secretsManagerClient: SecretsManagerClient): String {
        val valueString = secretsManagerClient.getSecretValue {
            it.secretId("websocket/subscribe/hmac")
        }.secretString()
        val hmacSecret = objectMapper.readValue(valueString, HmacSecret::class.java)
        return hmacSecret.secret
    }

    @Provides
    @Named(AVAILABLE_BALANCE_BUFFER)
    fun provideAvailableBalanceBuffer(): Double {
        return System.getenv("AVAILABLE_BALANCE_BUFFER")!!.toDouble().also {
            logger.info { "Got balance buffer $it" }
        }
    }

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "TransferHandler"

    @Provides
    @Named(TRANSFER_METADATA_QUEUE_URL)
    fun provideTransferMetadataQueueUrl(): String = System.getenv("ITEM_METADATA_QUEUE_URL")

    @Provides
    fun provideSqsClient(): SqsClient {
        return SqsClient.create()
    }

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }
}
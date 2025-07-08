package com.zenobiapay.transfertableevent.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.METRIC_NAMESPACE
import com.zenobiapay.table.model.HmacSecret
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import jakarta.inject.Named
import software.amazon.awssdk.services.ses.SesClient

const val WEBHOOK_KMS_ALIAS = "WEBHOOK_KMS_ALIAS"
const val TRANSFER_STATUS_NOTIFICATION_SECRET = "TRANSFER_STATUS_NOTIFICATION_SECRET"
const val WEBSOCKET_SERVICE_ENDPOINT = "WEBSOCKET_SERVICE_ENDPOINT"
const val IS_PROD = "IS_PROD"
const val SENDER_EMAIL = "NOTIFICATION_EMAIL"

private val logger = KotlinLogging.logger {}

@Module
class TransferTableEventModule {
    @Provides
    fun provideKmsClient(): KmsClient {
        return KmsClient.builder().build()
    }

    @Provides
    @Named(WEBHOOK_KMS_ALIAS)
    fun provideWebhookKmsAlias(): String {
        return System.getenv("WEBHOOK_KMS_ALIAS")
    }

    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    @Named(TRANSFER_STATUS_NOTIFICATION_SECRET)
    fun provideTransferNotificationSecret(objectMapper: ObjectMapper, secretsManagerClient: SecretsManagerClient): String {
        val valueString = secretsManagerClient.getSecretValue {
            it.secretId("websocket/update/hmac")
        }.secretString()
        val hmacSecret = objectMapper.readValue(valueString, HmacSecret::class.java)
        return hmacSecret.secret
    }

    @Provides
    @Named(WEBSOCKET_SERVICE_ENDPOINT)
    fun provideWebhookServiceEndpoint(): String {
        return System.getenv("WEBSOCKET_SERVICE_ENDPOINT")
    }

    @Provides
    @Named(IS_PROD)
    fun provideIsProd(): Boolean {
        return (System.getenv("STAGE") == "prod").also {
            logger.info { "Got isProd $it"}
        }
    }

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "TransferTableNotificationHandler"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }

    @Provides
    fun provideSesClient(): SesClient {
        return SesClient.builder()
            .build()
    }

    @Provides
    @Named(SENDER_EMAIL)
    fun provideSenderEmail(): String {
        return System.getenv("SENDER_EMAIL")
    }
}

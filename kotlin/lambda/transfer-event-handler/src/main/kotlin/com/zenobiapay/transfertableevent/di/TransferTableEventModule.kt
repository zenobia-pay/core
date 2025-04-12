package com.zenobiapay.transfertableevent.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.table.model.HmacSecret
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Named

const val WEBHOOK_KMS_ALIAS = "WEBHOOK_KMS_ALIAS"
const val TRANSFER_NOTIFICATION_SECRET = "TRANSFER_NOTIFICATION_SECRET"
const val WEBSOCKET_SERVICE_ENDPOINT = "WEBSOCKET_SERVICE_ENDPOINT"

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
    @Named(TRANSFER_NOTIFICATION_SECRET)
    fun provideTransferNotificationSecret(objectMapper: ObjectMapper, secretsManagerClient: SecretsManagerClient): String {
        val valueString = secretsManagerClient.getSecretValue {
            it.secretId("transfer/hmac")
        }.secretString()
        val hmacSecret = objectMapper.readValue(valueString, HmacSecret::class.java)
        return hmacSecret.secret
    }

    @Provides
    @Named(WEBSOCKET_SERVICE_ENDPOINT)
    fun provideWebhookServiceEndpoint(): String {
        return System.getenv("WEBSOCKET_SERVICE_ENDPOINT")
    }
}

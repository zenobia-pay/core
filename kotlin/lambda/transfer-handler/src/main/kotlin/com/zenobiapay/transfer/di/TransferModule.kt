package com.zenobiapay.transfer.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.table.model.HmacSecret
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Named

const val PAGINATION_SECRET = "PAGINATION_SECRET"
const val TRANSFER_NOTIFICATION_SECRET = "TRANSFER_NOTIFICATION_SECRET"
const val AVAILABLE_BALANCE_BUFFER = "AVAILABLE_BALANCE_BUFFER"

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
            it.secretId("transfer/hmac")
        }.secretString()
        val hmacSecret = objectMapper.readValue(valueString, HmacSecret::class.java)
        return hmacSecret.secret
    }

    @Provides
    @Named(AVAILABLE_BALANCE_BUFFER)
    fun provideAvailableBalanceBuffer(): Double {
        return System.getenv("AVAILABLE_BALANCE_BUFFER")!!.toDouble()
    }
}
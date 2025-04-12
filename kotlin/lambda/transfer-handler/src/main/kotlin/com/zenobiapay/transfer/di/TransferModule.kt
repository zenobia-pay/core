package com.zenobiapay.transfer.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Named

const val PAGINATION_SECRET = "PAGINATION_SECRET"
const val TRANSFER_NOTIFICATION_SECRET = "TRANSFER_NOTIFICATION_SECRET"

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
    fun provideTransferNotificationSecret(secretsManagerClient: SecretsManagerClient): String {
        return secretsManagerClient.getSecretValue {
            it.secretId("transfer/hmac")
        }.secretString()
    }
}
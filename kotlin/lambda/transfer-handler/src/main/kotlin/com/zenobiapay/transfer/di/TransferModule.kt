package com.zenobiapay.transfer.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class TransferModule {
    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }
}
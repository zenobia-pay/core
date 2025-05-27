package com.zenobiapay.transfermetadata.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class ItemMetadataModule {
    @Provides
    fun provideSecretsManager(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }
}
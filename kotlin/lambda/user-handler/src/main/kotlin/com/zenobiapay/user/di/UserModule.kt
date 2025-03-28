package com.zenobiapay.user.di

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Inject
import dagger.Module
import dagger.Provides

@Module
class UserModule {
    @Provides
    fun provideSecretsManager(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }
}
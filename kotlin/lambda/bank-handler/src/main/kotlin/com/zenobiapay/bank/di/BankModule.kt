package com.zenobiapay.bank.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Named

const val PAGINATION_SECRET = "PAGINATION_SECRET"

@Module
class BankModule {
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
}

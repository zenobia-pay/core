package com.zenobiapay.orum.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.OrumCredentials
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class OrumModule {
    @Provides
    fun provideOrumWrapper(httpClient: OkHttpClient, objectMapper: ObjectMapper, orumCredentials: OrumCredentials): OrumWrapper {
        return OrumWrapper(httpClient, objectMapper, orumCredentials)
    }

    @Provides
    fun provideOrumCredentials(secretsManagerClient: SecretsManagerClient, objectMapper: ObjectMapper): OrumCredentials {
        val secretString = secretsManagerClient.getSecretValue {
            it.secretId("orum/secrets")
        }.secretString()
        return objectMapper.readValue(secretString, OrumCredentials::class.java)
    }
}
package com.zenobiapay.bank.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.METRIC_NAMESPACE
import com.zenobiapay.table.credentials.dao.REFRESH_TOKEN_HASHING_SECRET
import com.zenobiapay.table.model.HmacSecret
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import jakarta.inject.Named

const val PAGINATION_SECRET = "PAGINATION_SECRET"
const val API_GATEWAY_ENDPOINT = "API_GATEWAY_ENDPOINT"

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

    @Provides
    @Named(REFRESH_TOKEN_HASHING_SECRET)
    fun provideRefreshTokenHashingSecret(objectMapper: ObjectMapper, secretsManagerClient: SecretsManagerClient): String {
        val secretString = secretsManagerClient.getSecretValue {
            it.secretId("credentials/refreshtoken/hmac")
        }.secretString()
        val hmacSecret = objectMapper.readValue(secretString, HmacSecret::class.java)
        return hmacSecret.secret
    }

    @Provides
    @Named(API_GATEWAY_ENDPOINT)
    fun provideApiGatewayEndpoint(): String {
        return System.getenv("API_GATEWAY_ENDPOINT")
    }

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "BankHandler"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }
}

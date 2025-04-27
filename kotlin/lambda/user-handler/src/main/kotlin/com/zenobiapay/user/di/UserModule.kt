package com.zenobiapay.user.di

import com.auth0.client.auth.AuthAPI
import com.auth0.client.mgmt.ManagementAPI
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobia.metric.METRIC_NAMESPACE
import com.zenobiapay.user.model.Auth0ManagementSecret
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import jakarta.inject.Named
import jakarta.inject.Singleton

@Module
class UserModule {
    companion object {
        const val AUTH_MANAGEMENT_TOKEN = "AUTH_MANAGEMENT_TOKEN"
        const val PAGINATION_SECRET = "PAGINATION_SECRET"
        const val PRIVACY_TERMS_VERSION = "PRIVACY_TERMS_VERSION"
        const val DEBIT_AUTH_VERSION = "DEBIT_AUTH_VERSION"
    }
    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    @Singleton
    fun provideAuth0ManagementSecret(secretsManager: SecretsManagerClient, objectMapper: ObjectMapper): Auth0ManagementSecret {
        val secrets = secretsManager.getSecretValue {
            it.secretId("auth0/secrets")
        }.secretString()
        return objectMapper.readValue(secrets, Auth0ManagementSecret::class.java)
    }

    @Provides
    fun provideAuthApi(auth0ManagementSecret: Auth0ManagementSecret): AuthAPI {
        return AuthAPI.newBuilder(
            auth0ManagementSecret.domain,
            auth0ManagementSecret.clientId,
            auth0ManagementSecret.clientSecret
        ).build()
    }

    @Provides
    @Named(AUTH_MANAGEMENT_TOKEN)
    fun provideAuthManagementToken(authAPI: AuthAPI, secret: Auth0ManagementSecret): String {
        return authAPI.requestToken(secret.audience).execute().body.accessToken
    }

    @Provides
    @Named(PAGINATION_SECRET)
    fun providePaginationSecret(secretsManagerClient: SecretsManagerClient): String {
        return secretsManagerClient.getSecretValue {
            it.secretId("pagination/hmac")
        }.secretString()
    }

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "UserHandler"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }

    @Provides
    @Named(PRIVACY_TERMS_VERSION)
    fun providePrivacyTermsVersion(): String {
        return System.getenv("PRIVACY_TERMS_VERSION")
    }

    @Provides
    @Named(DEBIT_AUTH_VERSION)
    fun provideDebitAuthVersion(): String {
        return System.getenv("DEBIT_AUTH_VERSION")
    }
}

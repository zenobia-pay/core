package com.zenobiapay.user.di

import com.auth0.client.auth.AuthAPI
import com.auth0.client.mgmt.ManagementAPI
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.user.model.Auth0ManagementSecret
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import javax.inject.Named
import javax.inject.Singleton

@Module
class UserModule {
    companion object {
        const val AUTH_MANAGEMENT_TOKEN = "AUTH_MANAGEMENT_TOKEN"
        const val PAGINATION_SECRET = "PAGINATION_SECRET"
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
    fun provideManagementAPI(
        auth0ManagementSecret: Auth0ManagementSecret,
        @Named(AUTH_MANAGEMENT_TOKEN) authManagementToken: String
    ): ManagementAPI {
        return ManagementAPI
            .newBuilder(auth0ManagementSecret.domain, authManagementToken)
            .build()
    }

    @Provides
    @Named(PAGINATION_SECRET)
    fun providePaginationSecret(secretsManagerClient: SecretsManagerClient): String {
        return secretsManagerClient.getSecretValue {
            it.secretId("pagination/hmac")
        }.secretString()
    }
}

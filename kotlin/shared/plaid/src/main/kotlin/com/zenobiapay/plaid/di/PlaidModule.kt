package com.zenobiapay.plaid.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.ApiClient
import com.plaid.client.request.PlaidApi
import com.zenobiapay.plaid.model.PlaidCredentials
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

private val logger = KotlinLogging.logger {}

@Module
class PlaidModule {
    @Provides
    fun providePlaidApi(plaidCredentials: PlaidCredentials): PlaidApi {
        val apiClient = ApiClient(plaidCredentials.toMap())
        if (plaidCredentials.endpoint == "/production") {
            logger.info { "Using production endpoint" }
            apiClient.setPlaidAdapter(ApiClient.Production)
        } else {
            logger.info { "Using sandbox endpoint" }
            apiClient.setPlaidAdapter(ApiClient.Sandbox)
        }

        return apiClient.createService(PlaidApi::class.java)
    }

    @Provides
    fun providePlaidCredentials(secretsManagerClient: SecretsManagerClient, objectMapper: ObjectMapper): PlaidCredentials {
        val secretString = secretsManagerClient.getSecretValue {
            it.secretId("plaid/secrets")
        }.secretString()
        return objectMapper.readValue(secretString, PlaidCredentials::class.java)
    }
}

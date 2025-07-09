package com.zenobiapay.plaid.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.ApiClient
import com.plaid.client.request.PlaidApi
import com.zenobiapay.plaid.model.PlaidCredentials
import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Named
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

private val logger = KotlinLogging.logger {}

const val IS_PLAID_SANDBOX = "IS_PLAID_SANDBOX"

@Module
class PlaidModule {
    @Provides
    fun providePlaidApi(plaidCredentials: PlaidCredentials, @Named(IS_PLAID_SANDBOX) isPlaidSandbox: Boolean): PlaidApi {
        val apiClient = ApiClient(plaidCredentials.toMap())
        if (isPlaidSandbox) {
            logger.info { "Using sandbox endpoint" }
            apiClient.setPlaidAdapter(ApiClient.Sandbox)
        } else {
            logger.info { "Using production endpoint" }
            apiClient.setPlaidAdapter(ApiClient.Production)
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

    @Provides
    @Named(IS_PLAID_SANDBOX)
    fun isPlaidSandbox(plaidCredentials: PlaidCredentials): Boolean {
        return plaidCredentials.endpoint != "/production"
    }
}

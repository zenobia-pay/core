package com.zenobiapay.plaid.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.ApiClient
import com.plaid.client.request.PlaidApi
import com.zenobiapay.plaid.model.PlaidCredentials
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class PlaidModule {
    @Provides
    fun providePlaidApi(plaidCredentials: PlaidCredentials): PlaidApi {
        val apiClient = ApiClient(plaidCredentials.toMap())
        apiClient.setPlaidAdapter(ApiClient.Sandbox) // TODO: make this interchangeable

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

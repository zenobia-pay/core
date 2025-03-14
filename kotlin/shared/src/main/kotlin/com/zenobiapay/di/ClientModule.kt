package com.zenobiapay.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.plaid.client.ApiClient
import com.plaid.client.request.PlaidApi
import com.zenobiapay.model.plaid.PlaidCredentials
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI
import javax.inject.Named

@Module
class ClientModule {
    @Provides
    fun provideDynamoDbClient(@Named(SAM_LOCAL) samLocal: String?): DynamoDbClient {
        val builder = DynamoDbClient.builder()
            .region(Region.US_EAST_1)
        if (samLocal != null) {
            builder.endpointOverride(URI("http://dynamodb-local:8000"))
        }
        return builder.build()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient) = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build()

    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    fun provideCognitoIdentityProvider(): CognitoIdentityProviderClient {
        return CognitoIdentityProviderClient.create()
    }

    @Provides
    fun provideSqsClient(): SqsClient {
        return SqsClient.create()
    }

    @Provides
    fun providePlaidApi(plaidCredentials: PlaidCredentials): PlaidApi {
        val apiClient = ApiClient(plaidCredentials.toMap())
        apiClient.setPlaidAdapter(ApiClient.Sandbox) // TODO: make this interchangeable

        return apiClient.createService(PlaidApi::class.java)
    }

    @Provides
    fun providePlaidCredentials(secretsManagerClient: SecretsManagerClient, objectMapper: ObjectMapper): PlaidCredentials {
        val secretString = secretsManagerClient.getSecretValue {
            it.secretId("plaid/sandbox/credentials")
        }.secretString()
        return objectMapper.readValue(secretString, PlaidCredentials::class.java)
    }

    @Provides
    fun provideHttpClient() = OkHttpClient() // TODO: add retries to client

    @Provides
    fun provideObjectMapper(): ObjectMapper = jacksonObjectMapper()
}

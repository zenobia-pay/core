package com.zenobiapay.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
    fun provideHttpClient() = OkHttpClient() // TODO: add retries to client

    @Provides
    fun provideObjectMapper(): ObjectMapper = jacksonObjectMapper()
}

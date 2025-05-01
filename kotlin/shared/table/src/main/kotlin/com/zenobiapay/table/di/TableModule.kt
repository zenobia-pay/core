package com.zenobiapay.table.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import jakarta.inject.Named
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryPolicy

const val BANK_TABLE_NAME = "BANK_TABLE_NAME"
const val TRANSFER_TABLE_NAME = "TRANSFER_TABLE_NAME"
const val USER_TABLE_NAME = "USER_TABLE_NAME"
const val CREDENTIALS_TABLE_NAME = "CREDENTIALS_TABLE_NAME"

@Module
class TableModule {
    @Provides
    @Named(BANK_TABLE_NAME)
    fun provideBankTableName(): String = System.getenv("BANK_TABLE_NAME")!!

    @Provides
    @Named(TRANSFER_TABLE_NAME)
    fun provideTransferTableName(): String = System.getenv("TRANSFER_TABLE_NAME")!!

    @Provides
    @Named(USER_TABLE_NAME)
    fun provideUserTableName(): String = System.getenv("USER_TABLE_NAME")!!

    @Provides
    @Named(CREDENTIALS_TABLE_NAME)
    fun provideCredentialsTableName(): String = System.getenv("CREDENTIALS_TABLE_NAME")!!

    @Provides
    fun provideDynamoDbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .retryPolicy(RetryPolicy.defaultRetryPolicy()
                        .toBuilder()
                        .numRetries(5)
                        .build()
                    ).build()
            )
            .region(Region.US_EAST_1)
            .build()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(dynamoDbClient: DynamoDbClient) = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build()
}

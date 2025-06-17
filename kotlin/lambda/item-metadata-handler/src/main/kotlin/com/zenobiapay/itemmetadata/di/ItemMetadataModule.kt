package com.zenobiapay.itemmetadata.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

const val METADATA_TRANSFORMER_LAMBDA_NAME = "METADATA_TRANSFORMER"

@Module
class ItemMetadataModule {
    @Provides
    fun provideSecretsManager(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    fun provideLambdaClient(): LambdaClient {
        return LambdaClient.create()
    }

    @Provides
    @Named(METADATA_TRANSFORMER_LAMBDA_NAME)
    fun provideMetadataTransformerLambdaName(): String = System.getenv("METADATA_TRANSFORMER_FUNCTION_NAME")
}
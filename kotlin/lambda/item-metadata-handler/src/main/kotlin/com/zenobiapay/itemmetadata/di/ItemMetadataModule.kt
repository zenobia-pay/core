package com.zenobiapay.itemmetadata.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import okhttp3.OkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.time.Duration
import javax.inject.Singleton

const val METADATA_TRANSFORMER_LAMBDA_NAME = "METADATA_TRANSFORMER"
const val ITEM_STORAGE_BUCKET_NAME = "ITEM_STORAGE_BUCKET_NAME"

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
    
    @Provides
    @Singleton
    fun provideS3Client(): S3Client {
        return S3Client.builder()
            .region(Region.US_EAST_1) // You can make this configurable if needed
            .build()
    }

    @Provides
    @Named(ITEM_STORAGE_BUCKET_NAME)
    fun provideImageStorageBucketName(): String {
        return System.getenv("IMAGE_STORAGE_BUCKET_NAME") ?: 
            throw IllegalStateException("IMAGE_STORAGE_BUCKET_NAME environment variable is not set")
    }
}
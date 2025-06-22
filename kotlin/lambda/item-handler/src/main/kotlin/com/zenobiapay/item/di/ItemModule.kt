package com.zenobiapay.item.di

import com.zenobia.metric.METRIC_NAMESPACE
import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class ItemModule {
    companion object {
        const val IMAGE_STORAGE_BUCKET_NAME = "IMAGE_STORAGE_BUCKET_NAME"
        const val RESALE_SERVICE_ENDPOINT = "RESALE_SERVICE_ENDPOINT"
        const val RESALE_SIGNING_SECRET = "RESALE_SIGNING_SECRET"
    }

    @Provides
    fun provideSecretsManager(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }
    
    @Provides
    fun provideS3Client(): S3Client {
        return S3Client.create()
    }
    
    @Provides
    @Named(IMAGE_STORAGE_BUCKET_NAME)
    fun provideImageStorageBucketName(): String {
        return System.getenv(IMAGE_STORAGE_BUCKET_NAME) ?: ""
    }

    @Provides
    @Named(RESALE_SERVICE_ENDPOINT)
    fun provideResaleServiceEndpoint(): String {
        return System.getenv("RESALE_SERVICE_ENDPOINT") ?: throw Exception("RESALE_SERVICE_ENDPOINT environment variable is not set")
    }

    @Provides
    @Named(RESALE_SIGNING_SECRET)
    fun provideResaleSigningSecret(): String {
        val secretName = System.getenv(RESALE_SIGNING_SECRET) ?: throw Exception("$RESALE_SIGNING_SECRET environment variable is not set")
        return provideSecretsManager().getSecretValue { it.secretId(secretName) }.secretString()
    }

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "ItemHandler"
}

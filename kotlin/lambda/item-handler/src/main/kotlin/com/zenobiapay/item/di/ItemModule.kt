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
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "ItemHandler"
}

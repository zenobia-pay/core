package com.zenobiapay.item.di

import com.zenobia.metric.METRIC_NAMESPACE
import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient

@Module
class ItemModule {
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
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "ItemHandler"
}

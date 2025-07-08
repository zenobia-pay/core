package com.zenobiapay.payout.di

import com.zenobia.metric.METRIC_NAMESPACE
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.sqs.SqsClient
import jakarta.inject.Named
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor

const val PAYOUT_QUEUE_URL = "PAYOUT_QUEUE_URL"

@Module
class PayoutModule {
    @Provides
    fun provideSqsClient(tracingInterceptor: ExecutionInterceptor): SqsClient {
        return SqsClient.builder()
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .addExecutionInterceptor(tracingInterceptor)
                    .build()
            )
            .build()
    }

    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    @Named(PAYOUT_QUEUE_URL)
    fun providePayoutQueueUrl(): String = System.getenv("PAYOUT_QUEUE_URL")

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "PayoutProcessor"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }
}
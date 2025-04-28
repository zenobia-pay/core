package com.zenobiapay.webhook.di

import com.zenobia.metric.METRIC_NAMESPACE
import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.sqs.SqsClient

const val ORUM_PUBLIC_CERTIFICATE = "ORUM_PUBLIC_CERTIFICATE"
const val ORUM_WEBHOOK_QUEUE = "ORUM_WEBHOOK_QUEUE_URL"

@Module
class EnvironmentModule {
    @Provides
    @Named(ORUM_PUBLIC_CERTIFICATE)
    fun provideOrumPublicCertificate(): String = System.getenv("ORUM_PUBLIC_CERTIFICATE")

    @Provides
    @Named(ORUM_WEBHOOK_QUEUE)
    fun provideOrumWebhookQueue(): String = System.getenv("ORUM_WEBHOOK_QUEUE_URL")

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "WebhookHandler"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }

    @Provides
    fun provideSqsClient(): SqsClient {
        return SqsClient.builder()
            .build()
    }
}
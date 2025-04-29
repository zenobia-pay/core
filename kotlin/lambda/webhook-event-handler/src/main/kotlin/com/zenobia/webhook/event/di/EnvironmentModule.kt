package com.zenobia.webhook.event.di

import com.zenobia.metric.METRIC_NAMESPACE
import dagger.Module
import dagger.Provides
import jakarta.inject.Named
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient

const val ORUM_SLACK_WEBHOOK = "ORUM_SLACK_WEBHOOK"
const val PLAID_SLACK_WEBHOOK = "PLAID_SLACK_WEBHOOK"

@Module
class EnvironmentModule {
    @Provides
    @Named(ORUM_SLACK_WEBHOOK)
    fun provideOrumSlackWebhook(): String = System.getenv("ORUM_SLACK_WEBHOOK")

    @Provides
    @Named(PLAID_SLACK_WEBHOOK)
    fun providePlaidSlackWebhook(): String = System.getenv("PLAID_SLACK_WEBHOOK")

    @Provides
    @Named(METRIC_NAMESPACE)
    fun provideMetricNamespace() = "WebhookEventHandler"

    @Provides
    fun provideCloudwatchClient(): CloudWatchClient {
        return CloudWatchClient.builder()
            .build()
    }
}
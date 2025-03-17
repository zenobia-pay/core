package com.zenobiapay.payout.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.sqs.SqsClient
import javax.inject.Named

const val PAYOUT_QUEUE_URL = "PAYOUT_QUEUE_URL"

@Module
class PayoutModule {
    @Provides
    fun provideSqsClient(): SqsClient {
        return SqsClient.create()
    }

    @Provides
    fun provideSecretsManagerClient(): SecretsManagerClient {
        return SecretsManagerClient.create()
    }

    @Provides
    @Named(PAYOUT_QUEUE_URL)
    fun providePayoutQueueUrl(): String = System.getenv("PAYOUT_QUEUE_URL")
}
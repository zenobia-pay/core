package com.zenobiapay.transfertableevent.di

import dagger.Module
import dagger.Provides
import software.amazon.awssdk.services.kms.KmsClient
import javax.inject.Named

const val WEBHOOK_KMS_ALIAS = "WEBHOOK_KMS_ALIAS"

@Module
class TransferTableEventModule {
    @Provides
    fun provideKmsClient(): KmsClient {
        return KmsClient.builder().build()
    }

    @Provides
    @Named(WEBHOOK_KMS_ALIAS)
    fun provideWebhookKmsAlias(): String {
        return System.getenv("WEBHOOK_KMS_ALIAS")
    }
}
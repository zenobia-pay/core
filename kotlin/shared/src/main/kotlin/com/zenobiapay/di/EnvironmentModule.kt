package com.zenobiapay.di

import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Named

const val SAM_LOCAL = "SAM_LOCAL"
const val BANK_TABLE_NAME = "BANK_TABLE_NAME"
const val TRANSFER_TABLE_NAME = "TRANSFER_TABLE_NAME"
const val USER_TABLE_NAME = "USER_TABLE_NAME"
const val USER_POOL_ID = "USER_POOL_ID"
const val PAYOUT_QUEUE_URL = "PAYOUT_QUEUE_URL"

private val logger = KotlinLogging.logger {}

@Module
class EnvironmentModule {
    @Provides
    @Named(SAM_LOCAL)
    fun provideSamLocal(): String? = System.getenv("AWS_SAM_LOCAL")

    @Provides
    @Named(USER_POOL_ID)
    fun provideUserPoolId(): String {
        val arn = System.getenv("COGNITO_USER_POOL_ARN")
        return arn.split("/").last().also {
            logger.info { "Got user pool id $it" }
        }
    }

    @Provides
    @Named(PAYOUT_QUEUE_URL)
    fun providePayoutQueueUrl(): String = System.getenv("PAYOUT_QUEUE_URL")
}

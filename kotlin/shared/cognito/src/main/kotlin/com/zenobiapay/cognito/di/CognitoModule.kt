package com.zenobiapay.cognito.di

import dagger.Module
import dagger.Provides
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import javax.inject.Named

private val logger = KotlinLogging.logger {}
const val USER_POOL_ID = "USER_POOL_ID"

@Module
class CognitoModule {
    @Provides
    @Named(USER_POOL_ID)
    fun provideUserPoolId(): String {
        val arn = System.getenv("COGNITO_USER_POOL_ARN")
        return arn.split("/").last().also {
            logger.info { "Got user pool id $it" }
        }
    }

    @Provides
    fun provideCognitoIdentityProvider(): CognitoIdentityProviderClient {
        return CognitoIdentityProviderClient.create()
    }
}
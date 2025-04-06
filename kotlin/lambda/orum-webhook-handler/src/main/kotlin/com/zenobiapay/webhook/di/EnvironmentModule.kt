package com.zenobiapay.webhook.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Named

const val ORUM_PUBLIC_CERTIFICATE = "ORUM_PUBLIC_CERTIFICATE"

@Module
class EnvironmentModule {
    @Provides
    @Named(ORUM_PUBLIC_CERTIFICATE)
    fun provideOrumPublicCertificate(): String = System.getenv("ORUM_PUBLIC_CERTIFICATE")
}
package com.zenobiapay.di

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Named

const val SAM_LOCAL = "SAM_LOCAL"

@Module
class SharedModule {
    @Provides
    fun provideHttpClient() = OkHttpClient() // TODO: add retries to client

    @Provides
    fun provideObjectMapper(): ObjectMapper = jacksonObjectMapper().registerKotlinModule()

    @Provides
    @Named(SAM_LOCAL)
    fun provideSamLocal(): String? = System.getenv("AWS_SAM_LOCAL")
}

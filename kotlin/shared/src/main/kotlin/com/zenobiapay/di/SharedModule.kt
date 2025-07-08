package com.zenobiapay.di

import com.amazonaws.xray.interceptors.TracingInterceptor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor
import javax.inject.Singleton

@Module
class SharedModule {
    @Provides
    fun provideHttpClient() = OkHttpClient() // TODO: add retries to client

    @Provides
    fun provideObjectMapper(): ObjectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    
    @Provides
    fun provideTracingInterceptor(): ExecutionInterceptor {
        return TracingInterceptor()
    }
}

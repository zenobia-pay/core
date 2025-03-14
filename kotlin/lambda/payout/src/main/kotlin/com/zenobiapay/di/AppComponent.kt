package com.zenobiapay.di

import com.zenobiapay.handlers.PayoutDispatcher
import com.zenobiapay.handlers.PayoutProcessor
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: PayoutDispatcher)
    fun inject(handler: PayoutProcessor)
}

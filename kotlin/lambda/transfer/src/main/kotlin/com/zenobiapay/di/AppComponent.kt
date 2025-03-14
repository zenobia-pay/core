package com.zenobiapay.di

import com.zenobiapay.handlers.TransferHandler
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: TransferHandler)
}

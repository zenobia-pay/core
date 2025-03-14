package com.zenobiapay.di

import dagger.Component
import main.kotlin.com.zenobiapay.handlers.TransferTableEventHandler

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: TransferTableEventHandler)
}

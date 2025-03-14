package com.zenobiapay.di

import com.zenobiapay.handlers.UserHandler
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: UserHandler)
}

package com.zenobiapay.m2mhandler.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.m2mhandler.handlers.M2MHandler
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    SharedModule::class,
    M2MModule::class,
])
interface AppComponent {
    fun inject(handler: M2MHandler)
}

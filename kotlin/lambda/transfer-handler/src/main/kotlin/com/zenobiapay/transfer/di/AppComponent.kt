package com.zenobiapay.transfer.di

import com.zenobiapay.di.ClientModule
import com.zenobiapay.di.EnvironmentModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.transfer.handlers.TransferHandler
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class, OrumModule::class])
interface AppComponent {
    fun inject(handler: TransferHandler)
}

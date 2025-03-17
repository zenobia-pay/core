package com.zenobiapay.transfertableevent.di

import com.zenobiapay.di.ClientModule
import com.zenobiapay.di.EnvironmentModule
import com.zenobiapay.table.di.TableModule
import dagger.Component
import main.kotlin.com.zenobiapay.handlers.TransferTableEventHandler

@Component(modules = [EnvironmentModule::class, ClientModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: TransferTableEventHandler)
}

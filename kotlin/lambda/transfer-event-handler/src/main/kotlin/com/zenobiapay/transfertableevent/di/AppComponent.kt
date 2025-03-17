package com.zenobiapay.transfertableevent.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.table.di.TableModule
import dagger.Component
import main.kotlin.com.zenobiapay.handlers.TransferTableEventHandler

@Component(modules = [SharedModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: TransferTableEventHandler)
}

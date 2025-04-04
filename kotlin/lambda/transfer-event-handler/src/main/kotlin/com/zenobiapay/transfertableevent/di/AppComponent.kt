package com.zenobiapay.transfertableevent.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.table.di.TableModule
import dagger.Component
import com.zenobiapay.transfertableevent.handlers.TransferTableEventHandler

@Component(modules = [SharedModule::class, TableModule::class, TransferTableEventModule::class])
interface AppComponent {
    fun inject(handler: TransferTableEventHandler)
}

package com.zenobiapay.transfer.di

import com.zenobiapay.cognito.di.CognitoModule
import com.zenobiapay.di.SharedModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.table.di.TableModule
import com.zenobiapay.transfer.handlers.TransferHandler
import dagger.Component

@Component(modules = [
    SharedModule::class,
    OrumModule::class,
    TableModule::class,
    CognitoModule::class,
    TransferModule::class,
])
interface AppComponent {
    fun inject(handler: TransferHandler)
}

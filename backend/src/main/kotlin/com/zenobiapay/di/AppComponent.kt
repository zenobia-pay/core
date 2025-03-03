package com.zenobiapay.di

import com.zenobiapay.handlers.*
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: BankHandler)
    fun inject(handler: TransferHandler)
    fun inject(handler: UserHandler)
    fun inject(handler: CognitoEventHandler)
    fun inject(handler: PayoutDispatcher)
    fun inject(handler: PayoutProcessor)
}

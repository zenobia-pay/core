package com.zenobiapay.di

import com.zenobiapay.handlers.BankHandler
import com.zenobiapay.handlers.CognitoEventHandler
import com.zenobiapay.handlers.CredentialsHandler
import com.zenobiapay.handlers.PayoutDispatcher
import com.zenobiapay.handlers.PayoutProcessor
import com.zenobiapay.handlers.TransferHandler
import com.zenobiapay.handlers.TransferTableEventHandler
import com.zenobiapay.handlers.UserHandler
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class])
interface AppComponent {
    fun inject(handler: BankHandler)
    fun inject(handler: TransferHandler)
    fun inject(handler: UserHandler)
    fun inject(handler: CognitoEventHandler)
    fun inject(handler: PayoutDispatcher)
    fun inject(handler: PayoutProcessor)
    fun inject(handler: TransferTableEventHandler)
    fun inject(handler: CredentialsHandler)
}

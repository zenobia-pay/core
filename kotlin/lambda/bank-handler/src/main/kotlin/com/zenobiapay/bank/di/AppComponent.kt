package com.zenobiapay.bank.di

import com.zenobiapay.bank.handlers.BankHandler
import com.zenobiapay.di.ClientModule
import com.zenobiapay.di.EnvironmentModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.plaid.di.PlaidModule
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class, OrumModule::class, PlaidModule::class])
interface AppComponent {
    fun inject(handler: BankHandler)
}
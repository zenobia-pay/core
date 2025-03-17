package com.zenobiapay.bank.di

import com.zenobiapay.bank.handlers.BankHandler
import com.zenobiapay.di.SharedModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.plaid.di.PlaidModule
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [
    SharedModule::class,
    OrumModule::class,
    PlaidModule::class,
    TableModule::class,
    BankModule::class,
])
interface AppComponent {
    fun inject(handler: BankHandler)
}
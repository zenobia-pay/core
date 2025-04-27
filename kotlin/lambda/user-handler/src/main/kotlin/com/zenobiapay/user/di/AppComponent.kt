package com.zenobiapay.user.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.plaid.di.PlaidModule
import com.zenobiapay.table.di.TableModule
import com.zenobiapay.user.handlers.UserHandler
import dagger.Component
import jakarta.inject.Singleton

@Singleton
@Component(modules = [SharedModule::class, TableModule::class, OrumModule::class, UserModule::class, PlaidModule::class])
interface AppComponent {
    fun inject(handler: UserHandler)
}

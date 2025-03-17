package com.zenobiapay.payout.di

import com.zenobiapay.di.ClientModule
import com.zenobiapay.di.EnvironmentModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.payout.handlers.PayoutDispatcher
import com.zenobiapay.payout.handlers.PayoutProcessor
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [EnvironmentModule::class, OrumModule::class, ClientModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: PayoutDispatcher)
    fun inject(handler: PayoutProcessor)
}

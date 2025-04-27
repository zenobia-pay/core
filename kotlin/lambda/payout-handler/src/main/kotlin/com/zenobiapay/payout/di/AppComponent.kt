package com.zenobiapay.payout.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.payout.handlers.PayoutProcessor
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [
    OrumModule::class,
    SharedModule::class,
    TableModule::class,
    PayoutModule::class,
])
interface AppComponent {
    fun inject(handler: PayoutProcessor)
}

package com.zenobiapay.webhook.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.plaid.di.PlaidModule
import com.zenobiapay.webhook.handlers.WebhookHandler
import dagger.Component

@Component(modules = [SharedModule::class, EnvironmentModule::class, PlaidModule::class])
interface AppComponent {
    fun inject(handler: WebhookHandler)
}

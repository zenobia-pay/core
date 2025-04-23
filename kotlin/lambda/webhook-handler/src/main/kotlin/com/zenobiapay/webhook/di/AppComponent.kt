package com.zenobiapay.webhook.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.table.di.TableModule
import com.zenobiapay.webhook.handlers.WebhookHandler
import dagger.Component

@Component(modules = [SharedModule::class, EnvironmentModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: WebhookHandler)
}

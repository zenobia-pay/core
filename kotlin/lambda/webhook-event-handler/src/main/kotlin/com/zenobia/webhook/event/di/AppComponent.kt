package com.zenobia.webhook.event.di

import com.zenobia.webhook.event.handlers.WebhookEventHandler
import com.zenobiapay.di.SharedModule
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [SharedModule::class, EnvironmentModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: WebhookEventHandler)
}

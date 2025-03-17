package com.zenobiapay.cognito.di

import com.zenobiapay.cognito.handlers.CognitoEventHandler
import com.zenobiapay.di.ClientModule
import com.zenobiapay.di.EnvironmentModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [EnvironmentModule::class, ClientModule::class, OrumModule::class, TableModule::class])
interface AppComponent {
    fun inject(handler: CognitoEventHandler)
}

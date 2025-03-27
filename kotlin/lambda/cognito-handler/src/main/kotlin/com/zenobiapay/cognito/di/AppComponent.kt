package com.zenobiapay.cognito.di

import com.zenobiapay.cognito.handlers.CustomerSignupEventHandler
import com.zenobiapay.di.SharedModule
import com.zenobiapay.orum.di.OrumModule
import com.zenobiapay.table.di.TableModule
import dagger.Component

@Component(modules = [
    SharedModule::class,
    OrumModule::class,
    TableModule::class,
    CognitoHandlerModule::class,
])
interface AppComponent {
    fun inject(handler: CustomerSignupEventHandler)
}

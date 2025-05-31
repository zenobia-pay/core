package com.zenobiapay.item.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.rds.di.RdsModule
import com.zenobiapay.item.handlers.ItemHandler
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [SharedModule::class, RdsModule::class, ItemModule::class])
interface AppComponent {
    fun inject(handler: ItemHandler)
}

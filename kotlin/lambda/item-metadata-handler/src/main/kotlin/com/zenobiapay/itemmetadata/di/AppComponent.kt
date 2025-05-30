package com.zenobiapay.itemmetadata.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.rds.di.RdsModule
import com.zenobiapay.itemmetadata.handlers.ItemMetadataHandler
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [SharedModule::class, RdsModule::class, ItemMetadataModule::class])
interface AppComponent {
    fun inject(handler: ItemMetadataHandler)
}

package com.zenobiapay.transfermetadata.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.rds.di.RdsModule
import com.zenobiapay.transfermetadata.handlers.ItemMetadataHandler
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [SharedModule::class, RdsModule::class, ItemMetadataModule::class])
interface AppComponent {
    fun inject(handler: ItemMetadataHandler)
}

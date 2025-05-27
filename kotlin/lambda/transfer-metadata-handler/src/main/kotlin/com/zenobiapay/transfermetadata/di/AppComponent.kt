package com.zenobiapay.transfertableevent.di

import com.zenobiapay.di.SharedModule
import com.zenobiapay.transfermetadata.handlers.TransferMetadataHandler
import dagger.Component

@Component(modules = [SharedModule::class])
interface AppComponent {
    fun inject(handler: TransferMetadataHandler)
}

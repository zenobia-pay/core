package com.zenobiapay.transfermetadata.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.zenobiapay.transfertableevent.di.DaggerAppComponent

class TransferMetadataHandler : RequestHandler<Map<String, Any>, Unit> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(
        p0: Map<String, Any>?,
        p1: Context?
    ) {
        TODO("Not yet implemented")
    }
}
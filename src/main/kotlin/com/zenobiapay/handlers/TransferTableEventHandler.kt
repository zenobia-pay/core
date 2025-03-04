package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.zenobiapay.di.DaggerAppComponent
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class TransferTableEventHandler : RequestHandler<DynamodbEvent, Unit> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: DynamodbEvent, context: Context?): Unit {
        logger.info { "Got event $event" }
    }
}
package main.kotlin.com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.logic.TransferTableEventLogic
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class TransferTableEventHandler : RequestHandler<DynamodbEvent, Unit> {
    init {
        DaggerAppComponent.create().inject(this)
    }

    @Inject
    lateinit var logic: TransferTableEventLogic

    override fun handleRequest(event: DynamodbEvent, context: Context?) {
        logger.info { "Got event $event" }
        event.records.forEach {
            logic.handleRecord(it)
        }
    }
}
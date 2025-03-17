package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.cognito.CognitoUtil
import com.zenobiapay.payout.di.DaggerAppComponent
import com.zenobiapay.payout.di.PAYOUT_QUEUE_URL
import com.zenobiapay.payout.model.PayoutMessage
import com.zenobiapay.payout.util.SqsUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class PayoutDispatcher : RequestHandler<Map<String, Any>, Unit> {
    @Inject
    lateinit var cognitoUtil: CognitoUtil

    @Inject
    lateinit var sqsUtil: SqsUtil

    @Inject
    @Named(PAYOUT_QUEUE_URL)
    lateinit var payoutQueueUrl: String

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: Map<String, Any>?, context: Context) {
        val date = LocalDateTime.now(ZoneOffset.UTC).minusDays(1) // TODO: fetch from eventbridge?
        cognitoUtil.listMerchantUserIds().forEach {
            logger.info { "Sending message to payout queue for merchant ${it.username()}" }
            val message = PayoutMessage(it.username(), date.toString())
            sqsUtil.sendMessage(
                message = objectMapper.writeValueAsString(message),
                queueUrl = payoutQueueUrl,
                context.awsRequestId
            )
        }
    }
}

package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.payout.di.DaggerAppComponent
import com.zenobiapay.payout.di.PAYOUT_QUEUE_URL
import com.zenobiapay.payout.model.PayoutMessage
import com.zenobiapay.payout.model.ScheduledEvent
import com.zenobiapay.payout.util.SqsUtil
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class PayoutDispatcher : RequestHandler<ScheduledEvent, Unit> {

    @Inject
    lateinit var sqsUtil: SqsUtil

    @Inject
    @Named(PAYOUT_QUEUE_URL)
    lateinit var payoutQueueUrl: String

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var userDao: UserDao

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: ScheduledEvent, context: Context) {
        logger.info { "Got event $event" }
        val utcInstant = Instant.parse(event.time)
        val date = LocalDateTime.ofInstant(utcInstant, ZoneId.of("UTC")).minusDays(1)
        logger.info { "Processing for date $date, calculated using event bridge time $utcInstant" }
        userDao.queryUsers(UserType.MERCHANT)?.forEach { page ->
            page.items().forEach {
                val merchantSub = it.getSub()
                logger.info { "Sending message to payout queue for merchant $merchantSub" }
                val message = PayoutMessage(merchantSub, date.toString())
                sqsUtil.sendMessage(
                    message = objectMapper.writeValueAsString(message),
                    queueUrl = payoutQueueUrl,
                    context.awsRequestId
                )
            }
        }
    }
}

package com.zenobiapay.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.zenobiapay.dao.TransferDao
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.di.DaggerAppComponent
import com.zenobiapay.logic.getFee
import com.zenobiapay.model.ddb.transfer.PayoutId
import com.zenobiapay.model.orum.OrumCreateTransferRequest
import com.zenobiapay.model.orum.TransferParticipant
import com.zenobiapay.model.sqs.PayoutMessage
import com.zenobiapay.util.OrumUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class PayoutProcessor : RequestHandler<SQSEvent, Unit> {

    @Inject
    lateinit var transferDao: TransferDao

    @Inject
    lateinit var orumUtil: OrumUtil

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: SQSEvent, context: Context?) {
        logger.info { "Got event $event" }
        logger.info { "Got context $context" }

        event.records.map {
            objectMapper.readValue(it.body, PayoutMessage::class.java)
        }.forEach {
            fulfillPayout(it)
        }
    }

    private fun fulfillPayout(message: PayoutMessage) {
        val payoutItem = transferDao.getPayoutItem(message.merchantId, message.date)
        if (payoutItem == null) {
            logger.info { "Did not find payout item for merchant ${message.merchantId}. Skipping paying out" }
            return
        }

        assert(payoutItem.data?.merchantPaid != true && payoutItem.data?.feePaid != true) {
            "Payout item should not have merchant paid or fee paid!"
        }

        val fee = getFee(payoutItem.amount)
        val merchantPayout = payoutItem.amount - fee

        // TODO: handle payouts less than 10 cents
        val transferResponse = orumUtil.createTransfer(
            OrumCreateTransferRequest(
                transferReferenceId = "${payoutItem.pk}#${payoutItem.sk}",
                amount = merchantPayout,
                destination = TransferParticipant(
                    customerReferenceId = message.merchantId,
                    accountReferenceId = "K8EBv4d93qirXZwZjzwmH9bPoyLEWvtRKvXa9", // TODO: fetch merchants preferred account
                    statementDisplayName = "ZP_${message.date}"
                )
            )
        )
        transferDao.updatePayoutMetadata(
            payoutItem,
            merchantPaid = true,
            merchantAmount = merchantPayout,
            merchantPayoutId = PayoutId(id = transferResponse.transfer.id),
            feePaid = false,
            feeAmount = fee,
        )
    }
}

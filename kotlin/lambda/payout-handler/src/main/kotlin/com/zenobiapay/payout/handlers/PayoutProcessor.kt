package com.zenobiapay.payout.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.model.ddb.transfer.PayoutId
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.payout.di.DaggerAppComponent
import com.zenobiapay.payout.model.PayoutMessage
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class MerchantNotConfiguredException: Exception("Merchant configuration not set!")

class PayoutProcessor : RequestHandler<SQSEvent, Unit> {

    @Inject
    lateinit var transferDao: TransferDao

    @Inject
    lateinit var orumWrapper: OrumWrapper

    @Inject
    lateinit var userDao: UserDao

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(event: SQSEvent, context: Context?) {
        logger.info { "Got event $event" }
        logger.info { "Got context $context" }

        event.records.map {
            objectMapper.readValue(it.body, PayoutMessage::class.java).also {
                logger.info { "Got record $it" }
            }
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
            "Payout item pk=${payoutItem.pk}, sk=${payoutItem.sk} should not have merchant paid or fee paid!"
        }

        val fee = getFee(payoutItem.amount)
        val merchantPayout = payoutItem.amount - fee

        val merchantData = userDao.getUserItem(message.merchantId)
        assert(merchantData?.userType == UserType.MERCHANT) {
            "Merchant data for merchant ${message.merchantId} does not exist or is not a merchant"
        }
        val merchantBankAccountId = merchantData?.data?.merchantData?.bankAccountId
        assert(merchantBankAccountId != null) { "Merchant bank account id is not specified" }

        // TODO: check the bank account is of correct type
        val bankAccountId = merchantData?.data?.merchantData?.bankAccountId ?: throw MerchantNotConfiguredException()

        // TODO: handle payouts less than 10 cents
        val transferResponse = orumWrapper.createTransfer(
            OrumCreateTransferRequest(
                transferReferenceId = "${payoutItem.pk}#${payoutItem.sk}",
                amount = merchantPayout,
                destination = TransferParticipant(
                    customerReferenceId = generateMerchantOrumId(message.merchantId),
                    accountReferenceId = bankAccountId,
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
            feeAmount = fee
        )
    }
}

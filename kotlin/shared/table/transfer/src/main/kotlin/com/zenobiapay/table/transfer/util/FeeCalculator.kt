package com.zenobiapay.table.transfer.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.also
import kotlin.times

private val logger = KotlinLogging.logger {}

private val percentFee = BigDecimal("0.01")
private const val fixedFee = 30

fun getFee(intAmount: Int): Int {
    assert(intAmount >= 0) { "Payout amount must be greater than 0." }

    val amount = BigDecimal(intAmount)
    val percentFeeAmount = amount.times(percentFee).setScale(0, RoundingMode.FLOOR).intValueExact()
    val feeCalculated = percentFeeAmount + fixedFee
    if (feeCalculated > intAmount) {
        logger.info { "Fee calculated $feeCalculated is greater than total amount $intAmount. Returning amount" }
        return intAmount
    }
    return feeCalculated.also {
        logger.info { "Calculated fee $it from total amount $intAmount" }
    }
}

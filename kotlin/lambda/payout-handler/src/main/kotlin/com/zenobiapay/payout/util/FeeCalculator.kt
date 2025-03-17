package com.zenobiapay.payout.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.also
import kotlin.times

private val logger = KotlinLogging.logger {}

private val percentFee = BigDecimal("0.01")
private const val fixedFee = 10

fun getFee(intAmount: Int): Int {
    assert(intAmount >= 0) { "Payout amount must be greater than 0." }

    val amount = BigDecimal(intAmount)
    val percentFeeAmount = amount.times(percentFee).setScale(0, RoundingMode.FLOOR).intValueExact()
    return (percentFeeAmount + fixedFee).also {
        logger.info { "Calculated fee $it from total amount $intAmount" }
    }
}

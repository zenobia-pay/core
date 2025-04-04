package com.zenobiapay.orum.util

// Used to prefix all references to Orum persons or businesses.
const val MERCHANT_ORUM_PREFIX = "MERCHANT"
const val CUSTOMER_ORUM_PREFIX = "CUSTOMER"

fun generateCustomerOrumId(customerId: String) = "$CUSTOMER_ORUM_PREFIX#$customerId"
fun generateMerchantOrumId(merchantId: String) = "$MERCHANT_ORUM_PREFIX#$merchantId"

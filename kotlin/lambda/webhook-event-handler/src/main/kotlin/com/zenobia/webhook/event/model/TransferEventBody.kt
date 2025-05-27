package com.zenobia.webhook.event.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class TransferEventBody(
    val transfer: TransferEventBodyInner
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransferEventBodyInner(
    val id: String,
    val transfer_reference_id: String,
    val amount: Int,
    val currency: String,
    val source: CustomerAccount?,
    val destination: CustomerAccount?,
    val status: String?,
    val account_statement_descriptor: String?,
    val speed: String?
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerAccount(
    val account_reference_id: String?,
    val customer_reference_id: String?,
    val statement_display_name: String?,
)

package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCreateTransferRequest(
    @JsonProperty("transfer_reference_id")
    val transferReferenceId: String,
    val amount: Int,
    val currency: String = "USD",
    val speed: String = "standard",
    val source: TransferParticipant? = null,
    val destination: TransferParticipant? = null,
    val accountStatementDescriptor: String? = null
)

data class TransferParticipant(
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("account_reference_id")
    val accountReferenceId: String,
    @JsonProperty("statement_display_name")
    val statementDisplayName: String? = null
)

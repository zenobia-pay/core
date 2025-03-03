package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrumCreateTransferResponse(
    val transfer: Transfer
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Transfer(
    val id: String,
    @JsonProperty("transfer_reference_id")
    val transferReferenceId: String,
    val amount: Int,
    val currency: String,
    val speed: String,
    val source: TransferParticipant?,
    val destination: TransferParticipant?,
    val status: OrumTransferStatus,
    @JsonProperty("status_reasons")
    val statusReasons: List<StatusReason>?,
)

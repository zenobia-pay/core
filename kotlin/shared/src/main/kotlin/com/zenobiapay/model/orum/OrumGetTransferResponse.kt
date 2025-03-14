package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrumGetTransferResponse(
    val transfer: Transfer
)

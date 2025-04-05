package com.zenobiapay.transfer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.zenobiapay.api.generated.model.FulfillTransferRequestSignature

@JsonPropertyOrder(alphabetic = true)
abstract class FulfillTransferRequestMixin {
    @JsonIgnore
    abstract fun getSignature(): FulfillTransferRequestSignature
}
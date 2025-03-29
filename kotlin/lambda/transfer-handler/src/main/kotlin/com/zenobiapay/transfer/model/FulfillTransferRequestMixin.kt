package com.zenobiapay.transfer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenobiapay.api.generated.models.FulfillTransferRequestSignature

abstract class FulfillTransferRequestMixin {
    @JsonIgnore
    abstract fun getFulfillTransferRequestSignature(): FulfillTransferRequestSignature
}
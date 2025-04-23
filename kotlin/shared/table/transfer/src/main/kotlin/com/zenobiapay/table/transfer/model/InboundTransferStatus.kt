package com.zenobiapay.table.transfer.model

import com.zenobiapay.api.generated.model.TransferStatus as ApiTransferStatus

enum class InboundTransferStatus {
    NOT_STARTED,
    IN_FLIGHT,
    COMPLETED,
    FAILED;

    fun toApiTransferStatus(): ApiTransferStatus {
        return when (this) {
            NOT_STARTED -> ApiTransferStatus.NOT_STARTED
            IN_FLIGHT -> ApiTransferStatus.IN_FLIGHT
            COMPLETED -> ApiTransferStatus.COMPLETED
            FAILED -> ApiTransferStatus.FAILED
        }
    }
}

package com.zenobiapay.table.transfer.model

import com.zenobiapay.api.generated.model.TransferStatus as ApiTransferStatus

enum class OutboundTransferStatus {
    NOT_STARTED,
    FULFILL_LOCKED,
    IN_FLIGHT,
    PAYOUT_LOCKED,
    COMPLETED,
    FAILED;

    fun toApiTransferStatus(): ApiTransferStatus {
        return when (this) {
            NOT_STARTED -> ApiTransferStatus.NOT_STARTED
            FULFILL_LOCKED -> ApiTransferStatus.NOT_STARTED
            IN_FLIGHT -> ApiTransferStatus.IN_FLIGHT
            PAYOUT_LOCKED -> ApiTransferStatus.IN_FLIGHT
            COMPLETED -> ApiTransferStatus.COMPLETED
            FAILED -> ApiTransferStatus.FAILED
        }
    }
}

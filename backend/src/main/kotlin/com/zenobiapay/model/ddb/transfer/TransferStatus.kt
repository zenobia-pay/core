package com.zenobiapay.model.ddb.transfer

import com.zenobiapay.model.api.transfer.TransferStatus

enum class TransferStatus {
    NOT_STARTED,
    IN_FLIGHT,
    COMPLETED,
    FAILED,
    CANCELLED;

    fun toApiTransferStatus(): TransferStatus {
        return when (this) {
            NOT_STARTED -> TransferStatus.NOT_STARTED
            IN_FLIGHT -> TransferStatus.IN_FLIGHT
            COMPLETED -> TransferStatus.COMPLETED
            FAILED -> TransferStatus.FAILED
            CANCELLED -> TransferStatus.CANCELLED
        }
    }
}

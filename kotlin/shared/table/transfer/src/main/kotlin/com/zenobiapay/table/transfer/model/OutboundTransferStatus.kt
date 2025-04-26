package com.zenobiapay.table.transfer.model

import com.zenobiapay.api.generated.model.TransferStatus as ApiTransferStatus

enum class OutboundTransferStatus {
    NOT_STARTED, // Transfer request was created but nobody has fulfilled it.
    FULFILL_LOCKED, // Modifying
    IN_FLIGHT_WAITING, // waiting for funds to come in
    IN_FLIGHT_APPROVED, // pre-approved go ahead!
    PAYOUT_LOCKED, // Lock before payout
    COMPLETED, // Payment to merchant complete
    FAILED; // Failed to payout.

    fun toApiTransferStatus(): ApiTransferStatus {
        return when (this) {
            NOT_STARTED -> ApiTransferStatus.NOT_STARTED
            FULFILL_LOCKED -> ApiTransferStatus.NOT_STARTED
            IN_FLIGHT_WAITING -> ApiTransferStatus.IN_FLIGHT
            IN_FLIGHT_APPROVED -> ApiTransferStatus.IN_FLIGHT
            PAYOUT_LOCKED -> ApiTransferStatus.IN_FLIGHT
            COMPLETED -> ApiTransferStatus.COMPLETED
            FAILED -> ApiTransferStatus.FAILED
        }
    }
}

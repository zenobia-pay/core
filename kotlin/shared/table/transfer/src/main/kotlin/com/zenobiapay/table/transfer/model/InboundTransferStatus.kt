package com.zenobiapay.table.transfer.model

import com.zenobiapay.api.generated.model.TransferStatus as ApiTransferStatus

enum class InboundTransferStatus(val order: Int) {
    NOT_STARTED(0), // Request is created, not sent.
    IN_FLIGHT(1), // Request has been fulfilled. We've sent the request to Orum.
    COMPLETED(2), // Request has been sent to ACH.
    SETTLED(3), // Funds are available in our FBO
    FAILED(4); // Something failed. Funds were not successfully sent to us.

    companion object {
        fun fromOrumTransferStatus(orumTransferStatus: String): InboundTransferStatus {
            return when (orumTransferStatus) {
                "pending", "created" -> IN_FLIGHT
                "completed" -> COMPLETED
                "settled" -> SETTLED
                "failed" -> FAILED
                else -> throw Exception("Unknown orum transfer status $orumTransferStatus")
            }
        }
    }

    fun toApiTransferStatus(): ApiTransferStatus {
        return when (this) {
            NOT_STARTED -> ApiTransferStatus.NOT_STARTED
            IN_FLIGHT -> ApiTransferStatus.IN_FLIGHT
            COMPLETED -> ApiTransferStatus.IN_FLIGHT
            SETTLED -> ApiTransferStatus.COMPLETED
            FAILED -> ApiTransferStatus.FAILED
        }
    }
}

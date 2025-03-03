package com.zenobiapay.model.api.transfer

import com.zenobiapay.model.api.ApiResponse

data class GetTransferResponse(
    val transferRequestId: String,
    val transferStatus: TransferStatus,
    val creditor: PaymentParticipantIdentity?,
    val debtor: PaymentParticipantIdentity?,
    val statementItems: List<StatementItem>,
    val status: TransferStatus,
    val statusMessage: String?,
): ApiResponse

data class PaymentParticipantIdentity(
    val id: String,
    val name: String,
)

enum class TransferStatus {
    NOT_STARTED,
    IN_FLIGHT,
    COMPLETED,
    FAILED,
    CANCELLED;
}

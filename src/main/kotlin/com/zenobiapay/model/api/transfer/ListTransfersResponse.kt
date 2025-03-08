package com.zenobiapay.model.api.transfer

import com.zenobiapay.generated.models.PaymentParticipantIdentity
import com.zenobiapay.generated.models.StatementItem
import com.zenobiapay.generated.models.TransferStatus
import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.ddb.transfer.TransferItem

data class ListTransfersResponse(
    val items: List<ListTransferItem>,
    val continuationToken: String? = null,
): ApiResponse

data class ListTransferItem(
    val amount: Int,
    val status: TransferStatus,
    val debtor: PaymentParticipantIdentity,
    val statementItems: List<StatementItem>,
    val creationTime: String,
) {
    companion object {
        fun fromTransferRequestItem(item: TransferItem): ListTransferItem {
            return ListTransferItem(
                amount = item.amount!!,
                status = item.status.toApiTransferStatus(),
                debtor = item.data!!.merchant!!.toApiParticipantIdentity(),
                statementItems = item.data?.statementItems?.map { it.toApiStatementItem() } ?: listOf(),
                creationTime = item.data!!.creationTime
            )
        }
    }
}

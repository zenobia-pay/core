package com.zenobiapay.transfertableevent.model

import com.zenobiapay.api.generated.models.TransferStatus

data class TransferWebhookBody(
    val transferRequestId: String,
    val amount: Int,
    val status: TransferStatus
)

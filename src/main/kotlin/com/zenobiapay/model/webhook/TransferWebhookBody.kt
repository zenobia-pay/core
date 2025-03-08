package com.zenobiapay.model.webhook

import com.zenobiapay.generated.models.TransferStatus

data class TransferWebhookBody(
    val transferRequestId: String,
    val amount: Int,
    val status: TransferStatus,
    val expiry: String,
)

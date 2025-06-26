package com.zenobiapay.transfertableevent.model

import com.zenobiapay.api.generated.model.TransferStatus

data class TransferWebhookBody(
    val transferRequestId: String,
    val amount: Int,
    val status: TransferStatus,
    val isTest: Boolean,
)

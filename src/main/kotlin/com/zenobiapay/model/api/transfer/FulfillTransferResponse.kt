package com.zenobiapay.model.api.transfer

import com.zenobiapay.model.api.ApiResponse

data class FulfillTransferResponse(
    val amount: Int,
    val statementItems: List<StatementItem>,
    val debtor: Debtor,
): ApiResponse

data class Debtor(
    val id: String,
    val name: String,
)

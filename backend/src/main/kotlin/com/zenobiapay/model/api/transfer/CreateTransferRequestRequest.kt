package com.zenobiapay.model.api.transfer

import com.fasterxml.jackson.databind.ObjectMapper

data class CreateTransferRequestRequest(
    val amount: Int = 0,
    val bankAccountId: String = "",
    val statementItems: List<StatementItem> = listOf()
) {
    companion object {
        fun from(request: String, objectMapper: ObjectMapper): CreateTransferRequestRequest {
            return objectMapper.readValue(request, CreateTransferRequestRequest::class.java)
        }
    }
}

data class StatementItem(
    val name: String,
    val amount: Int,
) {
    fun toDdbStatementItem(): com.zenobiapay.model.ddb.transfer.StatementItem {
        return com.zenobiapay.model.ddb.transfer.StatementItem(
            name = name,
            amount = amount
        )
    }
}

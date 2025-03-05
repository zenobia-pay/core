package com.zenobiapay.model.api.transfer

data class CreateTransferRequestRequest(
    val amount: Int = 0,
    val statementItems: List<StatementItem> = listOf(),
    val bankAccountId: String? = null,
)

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

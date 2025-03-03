package com.zenobiapay.model.api.bank

import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.ddb.bank.BankItem

data class ListBanksResponse(
    val continuationToken: String? = null,
    val items: List<ListBankItem>,
): ApiResponse

data class ListBankItem(
    val accountId: String?,
    val accountName: String?,
) {
    companion object {
        fun from(item: BankItem): ListBankItem {
            return ListBankItem(
                accountId = item.data.accountId,
                accountName = item.data.accountName,
            )
        }
    }
}

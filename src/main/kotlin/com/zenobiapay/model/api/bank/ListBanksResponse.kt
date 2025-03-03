package com.zenobiapay.model.api.bank

import com.zenobiapay.model.api.ApiResponse
import com.zenobiapay.model.ddb.bank.BankAccountItem

data class ListBanksResponse(
    val continuationToken: String? = null,
    val items: List<ListBankItem>,
): ApiResponse

data class ListBankItem(
    val accountId: String?,
    val accountName: String?,
) {
    companion object {
        fun from(item: BankAccountItem): ListBankItem {
            return ListBankItem(
                accountId = item.data.bankAccountId,
                accountName = item.data.bankAccountName,
            )
        }
    }
}

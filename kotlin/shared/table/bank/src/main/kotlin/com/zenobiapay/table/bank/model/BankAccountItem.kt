package com.zenobiapay.table.bank.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
data class BankAccountItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = "",
    var publicToken: String = "",
    @get:DynamoDbAttribute("data")
    var data: BankData = BankData()
) {
    companion object {
        fun generatePk(userId: String) = "BANK_ACCOUNT#c_$userId"

        fun generateSk(accountId: String) = "ID#$accountId"
    }
}

@DynamoDbBean
data class BankData(
    var bankAccountId: String = "",
    var bankAccountName: String = "",
    var bankAccountType: String = "",
    var orumId: String = "",
    var plaidItemId: String = ""
)

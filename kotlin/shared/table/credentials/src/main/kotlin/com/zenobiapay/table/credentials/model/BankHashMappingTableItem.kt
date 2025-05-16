package com.zenobiapay.table.credentials.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey

@DynamoDbBean
data class BankHashMappingTableItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    @get:DynamoDbSortKey
    var sk: String = "",
    var sub: String = ""
) {
    companion object {
        private const val BANK_HASH_PREFIX = "BANK_HASH#"

        fun generatePk(bankHash: String) = "$BANK_HASH_PREFIX$bankHash"
        fun generateSk() = "SUB"
    }
}

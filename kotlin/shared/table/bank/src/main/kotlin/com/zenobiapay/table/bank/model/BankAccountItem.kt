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
        fun generatePk(userId: String, deviceId: String?): String {
            val prefix = "BANK_ACCOUNT#c_$userId"
            if (deviceId != null) {
                return "${prefix}#d_$deviceId"
            }
            return prefix
        }

        fun generateSk(accountId: String) = "ID#$accountId"
    }
}

@DynamoDbBean
data class BankData(
    var bankAccountId: String = "",
    var bankAccountName: String = "",
    var bankAccountType: String = "",
    var orumId: String = "",
    var plaidItemId: String = "",
    var deviceCertificate: DeviceCertificate? = null,
    var bankPermissions: BankPermissions? = null,
)

enum class BankPermissions {
    RECEIVE_ONLY,
    SEND_ONLY,
}

@DynamoDbBean
data class DeviceCertificate(
    var certificateType: String,
    var certificateValue: String,
)

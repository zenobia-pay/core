package com.zenobiapay.table.credentials.model

import com.zenobiapay.table.util.signHmacSha256
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class CredentialsTableItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    var hashedRefreshToken: String = "",
) {
    companion object {
        fun generatePk(itemId: String) = "CRED#i_$itemId"
        fun hashRefreshToken(refreshToken: String, hmacSecret: String) = signHmacSha256(refreshToken, hmacSecret)
    }
}

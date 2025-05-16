package com.zenobiapay.table.credentials.model

import com.zenobiapay.table.util.signHmacSha256
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
data class CredentialsTableItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = ""
) {
    companion object {
        fun hashRefreshToken(refreshToken: String, hmacSecret: String) = signHmacSha256(refreshToken, hmacSecret)
    }
}

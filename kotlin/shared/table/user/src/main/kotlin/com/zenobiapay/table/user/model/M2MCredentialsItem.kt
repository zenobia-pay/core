package com.zenobiapay.table.user.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

data class M2MCredentialsItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    @get:DynamoDbSortKey
    var sk: String = "",
    @get:DynamoDbAttribute("data")
    var data: M2MCredentialsData = M2MCredentialsData(),
) {
    companion object {
        fun generatePk(userId: String) = "M2M#id_${userId}"
        fun generateSk(m2mClientId: String) = "CLIENT#${m2mClientId}"
    }
}

data class M2MCredentialsData(
    var auth0ClientName: String = "",
)
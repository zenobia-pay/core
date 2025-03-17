package com.zenobiapay.table.user.model

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
data class UserItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    @get:DynamoDbSortKey
    var sk: String = "",
    @get:DynamoDbAttribute("data")
    var data: UserItemData = UserItemData()
) {
    companion object {
        fun generatePk(sub: String) = "USER#id_$sub"
        fun generateSk() = "DETAILS"
    }
}

@DynamoDbBean
data class UserItemData(
    var orumPersonId: String = ""
)

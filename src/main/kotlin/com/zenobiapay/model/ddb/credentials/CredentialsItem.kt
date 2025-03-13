package com.zenobiapay.model.ddb.credentials

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey

@DynamoDbBean
data class CredentialsItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    var clientName: String = "",
    var sub: String = "",
)

package com.zenobiapay.model.ddb.transfer

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*
import java.time.Instant

@DynamoDbBean
data class TransferFulfillItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = "",
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_1]) var gsi1Pk: String = "",
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_1]) var gsi1Sk: String = "",
    var amount: Int = 0,
    var transferRequestId: String = "",
    var status: TransferStatus = TransferStatus.NOT_STARTED,
    var statusMessage: String = "",
    var deleted: Boolean = false,
    var ttl: String? = null,
    var data: TransferData? = null,
    var version: Int = 0,
) {
    companion object { // TODO: combine fulfill and request items
        const val GSI_1 = "GSI1"
        fun generatePk(creditorId: String) = "FULFILL#c_$creditorId"
        fun generateSk(requestId: String) = requestId
        fun generateGsi1Pk(creditorId: String) = "FULFILL#c_$creditorId"
        fun generateGsi1Sk(requestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$requestId"
    }
}

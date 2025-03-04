package com.zenobiapay.model.ddb.transfer

import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant

@DynamoDbBean
data class TransferItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = "",
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_1]) var gsi1Pk: String? = null,
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_1]) var gsi1Sk: String? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_2]) var gsi2Pk: String? = null,
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_2]) var gsi2Sk: String? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_3]) var gsi3Pk: String? = null,
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_3]) var gsi3Sk: String? = null,
    var amount: Int? = null,
    var status: TransferStatus = TransferStatus.NOT_STARTED,
    var transferFulfillId: String? = null,
    var deleted: Boolean = false,
    var ttl: String? = null,
    var data: TransferData? = null,
    @get:DynamoDbVersionAttribute var version: Int? = null,
) {
    val requestId: String
        get() = sk
    companion object {
        const val GSI_1 = "GSI1"
        const val GSI_2 = "GSI2"
        const val GSI_3 = "GSI3"
        fun generatePk(merchantId: String) = "REQUEST#m_$merchantId"
        fun generateSk(requestId: String) = requestId
        fun generateGsi1Pk(merchantId: String) = "REQUEST#s_$merchantId"
        fun generateGsi1Sk(transferRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$transferRequestId"

        // Queries for customer
        fun generateGsi2Pk(customerId: String) = "FULFILL#c_$customerId"
        fun generateGsi2Sk(fulfillRequestId: String) = fulfillRequestId
        fun generateGsi3Pk(customerId: String) = "FULFILL#c_$customerId"
        fun generateGsi3Sk(fulfillRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$fulfillRequestId"
    }
}

@DynamoDbBean
data class TransferData(
    var customer: PaymentParticipantIdentity? = null,
    var merchant: PaymentParticipantIdentity? = null,
    var statementItems: List<StatementItem> = listOf(),
    var statusMessage: String? = null,
    var creationTime: String = "",
    var webhookUrl: String? = null,
)

@DynamoDbBean
data class PaymentParticipantIdentity(
    var id: String = "",
    var name: String = "",
    var bankAccountId: String = "",
) {
    fun toApiParticipantIdentity(): com.zenobiapay.model.api.transfer.PaymentParticipantIdentity {
        return com.zenobiapay.model.api.transfer.PaymentParticipantIdentity(
            id = this.id,
            name = this.name,
        )
    }
}

@DynamoDbBean
data class StatementItem(
    var name: String = "",
    var amount: Int = 0,
) {
    fun toApiStatementItem(): com.zenobiapay.model.api.transfer.StatementItem {
        return com.zenobiapay.model.api.transfer.StatementItem(
            name = name,
            amount = amount,
        )
    }
}

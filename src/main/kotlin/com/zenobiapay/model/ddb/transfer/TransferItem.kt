package com.zenobiapay.model.ddb.transfer

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.zenobiapay.model.exception.InvalidRequestException
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant
import com.zenobiapay.generated.models.PaymentParticipantIdentity as ApiPaymentParticipantIdentity

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
    var ttl: Int? = null,
    var data: TransferData? = null,
    @get:DynamoDbVersionAttribute var version: Int? = null
) {
    val requestId: String
        get() = sk
    companion object {
        const val GSI_1 = "GSI1"
        const val GSI_2 = "GSI2"
        const val GSI_3 = "GSI3"
        const val PK_PREFIX = "TRANSFER"
        fun generatePk(merchantId: String) = "$PK_PREFIX#m_$merchantId"
        fun generateSk(requestId: String) = requestId
        fun generateGsi1Pk(merchantId: String) = "$PK_PREFIX#m_$merchantId"
        fun generateGsi1Sk(transferRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$transferRequestId"

        // Queries for customer
        fun generateGsi2Pk(customerId: String) = "$PK_PREFIX#c_$customerId"
        fun generateGsi2Sk(fulfillRequestId: String) = fulfillRequestId
        fun generateGsi3Pk(customerId: String) = "$PK_PREFIX#c_$customerId"
        fun generateGsi3Sk(fulfillRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$fulfillRequestId"

        fun fromAttributeValueMap(map: Map<String, AttributeValue>): TransferItem {
            return TransferItem(
                pk = map["pk"]!!.s,
                sk = map["sk"]!!.s,
                gsi1Pk = map["gsi1Pk"]?.s,
                gsi1Sk = map["gsi1Sk"]?.s,
                gsi2Pk = map["gsi2Pk"]?.s,
                gsi2Sk = map["gsi2Sk"]?.s,
                gsi3Pk = map["gsi3Pk"]?.s,
                gsi3Sk = map["gsi3Sk"]?.s,
                amount = map["amount"]!!.n.toInt(),
                status = TransferStatus.valueOf(map["status"]!!.s),
                transferFulfillId = map["transferFulfillId"]?.s,
                deleted = map["deleted"]!!.bool,
                ttl = map["ttl"]?.n?.toInt(),
                data = TransferData.fromAttributeValueMap(map["data"]!!.m),
                version = map["version"]!!.n.toInt()
            )
        }
    }
}

@DynamoDbBean
data class TransferData(
    var customer: PaymentParticipantIdentity? = null,
    var merchant: PaymentParticipantIdentity? = null,
    var statementItems: List<StatementItem> = listOf(),
    var statusMessage: String? = null,
    var creationTime: String = "",
    var webhookUrl: String? = null
) {
    companion object {
        fun fromAttributeValueMap(map: Map<String, AttributeValue>): TransferData {
            val customerMap = map["customer"]?.m
            val merchantMap = map["merchant"]!!.m

            val customerIdentity = if (customerMap != null) {
                PaymentParticipantIdentity.fromAttributeValueMap(customerMap)
            } else {
                null
            }

            val merchantIdentity = PaymentParticipantIdentity.fromAttributeValueMap(merchantMap)

            return TransferData(
                customer = customerIdentity,
                merchant = merchantIdentity,
                statementItems = map["statementItems"]!!.l.map { StatementItem.fromAttributeValueMap(it.m) },
                webhookUrl = map["webhookUrl"]?.s
            )
        }
    }
}

@DynamoDbBean
data class PaymentParticipantIdentity(
    var id: String = "",
    var name: String = "",
    var bankAccountId: String = ""
) {
    companion object {
        fun fromAttributeValueMap(map: Map<String, AttributeValue>): PaymentParticipantIdentity {
            return PaymentParticipantIdentity(
                id = map["id"]!!.s,
                name = map["name"]!!.s,
                bankAccountId = map["bankAccountId"]!!.s
            )
        }
    }

    fun toApiParticipantIdentity(): ApiPaymentParticipantIdentity {
        return ApiPaymentParticipantIdentity(
            id = this.id,
            name = this.name
        )
    }
}

@DynamoDbBean
data class StatementItem(
    var name: String = "",
    var amount: Int = 0
) {
    companion object {
        fun fromAttributeValueMap(map: Map<String, AttributeValue>): StatementItem {
            return StatementItem(
                name = map["name"]!!.s,
                amount = map["amount"]!!.n.toInt()
            )
        }

        fun fromApiRequestStatementItem(item: com.zenobiapay.generated.models.StatementItem) =
            StatementItem(
                name = item.name ?: throw InvalidRequestException("name not specified in statementItems"),
                amount = item.amount ?: throw InvalidRequestException("item amount not specified in statementItems")
            )
    }

    fun toApiStatementItem(): com.zenobiapay.generated.models.StatementItem {
        return com.zenobiapay.generated.models.StatementItem(
            name = name,
            amount = amount
        )
    }
}

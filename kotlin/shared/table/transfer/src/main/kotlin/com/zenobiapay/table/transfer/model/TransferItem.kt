package com.zenobiapay.table.transfer.model

import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.zenobiapay.api.model.exception.InvalidRequestException
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import java.time.Instant
import com.zenobiapay.api.generated.model.PaymentParticipantIdentity as ApiPaymentParticipantIdentity

@DynamoDbBean
data class TransferItem(
    @get:DynamoDbPartitionKey var pk: String = "",
    @get:DynamoDbSortKey var sk: String = "",
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_1]) var gsi1Pk: String? = null,
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_1]) var gsi1Sk: String? = null,
    @get:DynamoDbSecondaryPartitionKey(indexNames = [GSI_2]) var gsi2Pk: String? = null,
    @get:DynamoDbSecondarySortKey(indexNames = [GSI_2]) var gsi2Sk: String? = null,
    var amount: Int? = null,
    var inboundStatus: InboundTransferStatus = InboundTransferStatus.NOT_STARTED,
    var outboundStatus: OutboundTransferStatus = OutboundTransferStatus.NOT_STARTED,
    var riskScore: Int? = null,
    var transferFulfillId: String? = null,
    var deleted: Boolean = false,
    var ttl: Int? = null,
    var data: TransferData? = null,
    @get:DynamoDbVersionAttribute var version: Int? = null
) {
    val requestId: String
        get() = pk.removePrefix("$PK_PREFIX#id_")
    companion object {
        const val GSI_1 = "GSI1"
        const val GSI_2 = "GSI2"
        const val PK_PREFIX = "TRANSFER"
        fun generatePk(requestId: String) = "$PK_PREFIX#id_$requestId"
        fun generateSk() = "DETAILS"
        fun generateGsi1Pk(merchantId: String) = "$PK_PREFIX#m_$merchantId"
        fun generateGsi1Sk(transferRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$transferRequestId"

        // Queries for customer
        fun generateGsi2Pk(customerId: String) = "$PK_PREFIX#c_$customerId"
        fun generateGsi2Sk(fulfillRequestId: String, timestamp: Instant) = "CREATED#t_$timestamp#id_$fulfillRequestId"

        fun fromAttributeValueMap(map: Map<String, AttributeValue>): TransferItem {
            return TransferItem(
                pk = map["pk"]!!.s,
                gsi1Pk = map["gsi1Pk"]?.s,
                gsi1Sk = map["gsi1Sk"]?.s,
                gsi2Pk = map["gsi2Pk"]?.s,
                gsi2Sk = map["gsi2Sk"]?.s,
                amount = map["amount"]!!.n.toInt(),
                inboundStatus = InboundTransferStatus.valueOf(map["inboundStatus"]!!.s),
                outboundStatus = OutboundTransferStatus.valueOf(map["outboundStatus"]!!.s),
                riskScore = map["riskScore"]?.n?.toInt(),
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
    var fee: Int? = null,
    var orumPayoutId: String? = null,
    var customerBankAccount: BankAccount? = null,
    var statementItems: List<StatementItem> = listOf(),
    var statusMessage: String? = null,
    var creationTime: String = "",
    var webhookUrl: String? = null,
    var signature: Signature? = null,
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
data class Signature(
    var signatureType: String = "",
    var signature: String = "",
)

@DynamoDbBean
data class BankAccount(
    var name: String = "",
    var id: String = "",
    var lastFourDigits: String = "",
)

@DynamoDbBean
data class PaymentParticipantIdentity(
    var id: String = "",
    var name: String? = null,
    var bankAccountId: String = ""
) {
    companion object {
        fun fromAttributeValueMap(map: Map<String, AttributeValue>): PaymentParticipantIdentity {
            return PaymentParticipantIdentity(
                id = map["id"]!!.s,
                name = map["name"]?.s,
                bankAccountId = map["bankAccountId"]!!.s
            )
        }
    }

    fun toApiParticipantIdentity(): ApiPaymentParticipantIdentity {
        return ApiPaymentParticipantIdentity().id(this.id).name(this.name)
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

        fun fromApiRequestStatementItem(item: com.zenobiapay.api.generated.model.StatementItem) =
            StatementItem(
                name = item.name ?: throw InvalidRequestException("name not specified in statementItems"),
                amount = item.amount ?: throw InvalidRequestException("item amount not specified in statementItems")
            )
    }

    fun toApiStatementItem(): com.zenobiapay.api.generated.model.StatementItem {
        return com.zenobiapay.api.generated.model.StatementItem().name(name).amount(amount)
    }
}

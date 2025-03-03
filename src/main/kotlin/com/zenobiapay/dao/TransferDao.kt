package com.zenobiapay.dao

import com.zenobiapay.di.TRANSFER_TABLE_NAME
import com.zenobiapay.model.api.transfer.StatementItem
import com.zenobiapay.model.ddb.transfer.*
import com.zenobiapay.util.MAX_BANK_ITEMS
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class TransferDao @Inject constructor(
    private val client: DynamoDbEnhancedClient,
    private val lowLevelClient: DynamoDbClient,
    @Named(TRANSFER_TABLE_NAME)
    private val transferTableName: String
) {
    private val transferRequestTable = client.table(transferTableName, TableSchema.fromBean(TransferRequestItem::class.java))
    private val transferFulfillTable = client.table(transferTableName, TableSchema.fromBean(TransferFulfillItem::class.java))
    private val payoutTable = client.table(transferTableName, TableSchema.fromBean(PayoutItem::class.java))

    fun putTransferRequest(merchantId: String, requestId: String, amountInCents: Int, merchantName: String, statementItems: List<StatementItem>) {
        val pk = TransferRequestItem.generatePk(merchantId)
        val sk = TransferRequestItem.generateSk(requestId)
        val gsi1Pk = TransferRequestItem.generateGsi1Pk(merchantId)
        val gsi1Sk = TransferRequestItem.generateGsi1Sk(requestId, Instant.now())
        val creationTime = Instant.now()
        transferRequestTable.putItem(
            TransferRequestItem(
                pk = pk,
                sk = sk,
                gsi1Pk = gsi1Pk,
                gsi1Sk = gsi1Sk,
                amount = amountInCents,
                data = TransferData(
                    statementItems = statementItems.map { it.toDdbStatementItem() },
                    merchant = PaymentParticipantIdentity(
                        id = merchantId,
                        name = merchantName
                    ),
                    creationTime = creationTime.toString()
                )
            )
        )
    }

    fun updateTransferRequest(
        transferRequestItem: TransferRequestItem,
        fulfillRequestId: String,
        customerIdentity: PaymentParticipantIdentity,
    ) {
        val updatedItem = transferRequestItem.copy(
            status = TransferStatus.IN_FLIGHT,
            transferFulfillId = fulfillRequestId,
            data = transferRequestItem.data?.copy(
                customer = customerIdentity,
            )
        )

        val request = UpdateItemEnhancedRequest.builder(TransferRequestItem::class.java)
            .item(updatedItem)
            .build()

        transferRequestTable.updateItem(request)
        // TODO: handle Conditional check failed from optimistic version lock
    }

    fun putTransferFulfill(
        customerIdentity: PaymentParticipantIdentity,
        merchantIdentity: PaymentParticipantIdentity,
        requestId: String,
        transferRequestId: String,
        amountInCents: Int,
        statementItems: List<StatementItem>
    ) {
        val table = client.table(transferTableName, TableSchema.fromBean(TransferFulfillItem::class.java))
        val fulfillTime = Instant.now()
        val pk = TransferFulfillItem.generatePk(customerIdentity.id)
        val sk = TransferFulfillItem.generateSk(requestId)
        val gsi1Pk = TransferFulfillItem.generateGsi1Pk(customerIdentity.id)
        val gsi1Sk = TransferFulfillItem.generateGsi1Sk(requestId, fulfillTime)
        table.putItem(
            TransferFulfillItem(
                pk = pk,
                sk = sk,
                gsi1Pk = gsi1Pk,
                gsi1Sk = gsi1Sk,
                status = TransferStatus.COMPLETED,
                amount = amountInCents,
                data = TransferData(
                    statementItems = statementItems.map { it.toDdbStatementItem() },
                    customer = customerIdentity,
                    merchant = merchantIdentity,
                    creationTime = fulfillTime.toString(),
                ),
                transferRequestId = transferRequestId,
            )
        )
    }

    fun getTransferRequest(merchantId: String, transferRequestId: String): TransferRequestItem {
        val table = client.table(transferTableName, TableSchema.fromBean(TransferRequestItem::class.java))
        val pk = TransferRequestItem.generatePk(merchantId)
        val sk = TransferRequestItem.generateSk(transferRequestId)
        return table.getItem {
            it.key {
                it.partitionValue(pk).sortValue(sk)
            }
        }
    }

    fun listCustomerTransfers(customerId: String): List<TransferFulfillItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(TransferFulfillItem.generateGsi1Pk(customerId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_BANK_ITEMS)
            .build()

        // TODO: handle pagination
        val toReturn = mutableListOf<TransferFulfillItem>()
        transferFulfillTable.index(TransferFulfillItem.GSI_1)
            .query(queryRequest)
            .stream().forEach {
                logger.info { "Got list response page ${it.items()}" }
                toReturn += it.items()
            }

        logger.info { "Returning accumulated list $toReturn" }
        return toReturn
    }

    fun addPayoutItemAmount(merchantId: String, amount: Int, date: LocalDate) {
        val pk = PayoutItem.generatePk(merchantId)
        val sk = PayoutItem.generateSk(date)

        val updateRequest = UpdateItemRequest.builder()
            .tableName(transferTableName)
            .key(mapOf("pk" to AttributeValue.builder().s(pk).build(), "sk" to AttributeValue.builder().s(sk).build()))
            .updateExpression("SET amount = if_not_exists(amount, :0) + :inc")
            .expressionAttributeValues(
                mapOf(
                    ":0" to AttributeValue.builder().n("0").build(),
                    ":inc" to AttributeValue.builder().n(amount.toString()).build(),
                )
            ).build()
        logger.info { "Calling payout item ddb update using request $updateRequest" }
        lowLevelClient.updateItem(updateRequest)
    }

    fun updatePayoutMetadata(
        item: PayoutItem,
        merchantPaid: Boolean = false,
        merchantPayoutId: PayoutId? = null,
        merchantAmount: Int? = null,
        feePaid: Boolean = false,
        feeAmount: Int? = null,
        feePayoutId: PayoutId? = null,
    ) {
        val updatedItem = item.copy(
            data = PayoutData(
                merchantPaid = merchantPaid,
                merchantAmount = merchantAmount,
                merchantPayoutId = merchantPayoutId,
                feePaid = feePaid,
                feeAmount = feeAmount,
                feePayoutId = feePayoutId,
            )
        )
        payoutTable.updateItem(updatedItem)
        logger.info { "Updated payout metadat with item $item" }
    }

    fun getPayoutItem(merchantId: String, date: String): PayoutItem? {
        val pk = PayoutItem.generatePk(merchantId)
        val sk = PayoutItem.generateSk(date)
        return try {
            payoutTable.getItem {
                it.key {
                    it.partitionValue(pk)
                        .sortValue(sk)
                }
            }.also {
                logger.info { "Found payout item $it" }
            }
        } catch (e: ResourceNotFoundException) {
            logger.info { "Did not find payout item for merchant $merchantId, date $date" }
            null
        }
    }
}
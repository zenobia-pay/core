package com.zenobiapay.dao

import com.zenobiapay.di.TRANSFER_TABLE_NAME
import com.zenobiapay.model.ddb.transfer.*
import com.zenobiapay.model.ddb.transfer.TransferItem.Companion.GSI_1
import com.zenobiapay.model.ddb.transfer.TransferItem.Companion.GSI_2
import com.zenobiapay.util.MAX_LIST_ITEMS
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
    private val transferTable = client.table(transferTableName, TableSchema.fromBean(TransferItem::class.java))
    private val payoutTable = client.table(transferTableName, TableSchema.fromBean(PayoutItem::class.java))

    fun putTransferRequest(merchantId: String, requestId: String, amountInCents: Int, merchantName: String, statementItems: List<StatementItem>) {
        val pk = TransferItem.generatePk(merchantId)
        val sk = TransferItem.generateSk(requestId)
        val gsi1Pk = TransferItem.generateGsi1Pk(merchantId)
        val gsi1Sk = TransferItem.generateGsi1Sk(requestId, Instant.now())
        val creationTime = Instant.now()
        transferTable.putItem(
            TransferItem(
                pk = pk,
                sk = sk,
                gsi1Pk = gsi1Pk,
                gsi1Sk = gsi1Sk,
                amount = amountInCents,
                data = TransferData(
                    statementItems = statementItems,
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
        transferItem: TransferItem,
        fulfillRequestId: String,
        customerIdentity: PaymentParticipantIdentity,
        timestamp: Instant,
        webhookUrl: String?
    ) {
        val updatedItem = transferItem.copy(
            status = TransferStatus.IN_FLIGHT,
            transferFulfillId = fulfillRequestId,
            data = transferItem.data?.copy(
                customer = customerIdentity,
                webhookUrl = webhookUrl
            ),
            gsi2Pk = TransferItem.generateGsi2Pk(customerIdentity.id),
            gsi2Sk = TransferItem.generateGsi2Sk(fulfillRequestId),
            gsi3Pk = TransferItem.generateGsi3Pk(customerIdentity.id),
            gsi3Sk = TransferItem.generateGsi3Sk(fulfillRequestId, timestamp)
        )

        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(updatedItem)
            .build()

        transferTable.updateItem(request)
        // TODO: handle Conditional check failed from optimistic version lock
    }

    fun getCustomerTransfer(customerId: String, fulfillRequestId: String): TransferItem? {
        val pk = TransferItem.generateGsi2Pk(customerId)
        val sk = TransferItem.generateGsi2Sk(fulfillRequestId)

        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(pk)
                .sortValue(sk)
        }

        return transferTable.index(GSI_2).query(
            QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build()
        ).first().items().firstOrNull()
    }

    fun getMerchantTransfer(merchantId: String, transferRequestId: String): TransferItem {
        val pk = TransferItem.generatePk(merchantId)
        val sk = TransferItem.generateSk(transferRequestId)
        return transferTable.getItem {
            it.key {
                it.partitionValue(pk).sortValue(sk)
            }
        }
    }

    fun listCustomerTransfers(customerId: String): List<TransferItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(TransferItem.generateGsi3Pk(customerId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)
            .build()

        // TODO: handle pagination
        val toReturn = mutableListOf<TransferItem>()
        transferTable.index(TransferItem.GSI_3)
            .query(queryRequest)
            .stream().forEach {
                logger.info { "Got list response page ${it.items()}" }
                toReturn += it.items()
            }

        logger.info { "Returning accumulated list $toReturn" }
        return toReturn
    }

    fun listMerchantTransfers(merchantId: String): List<TransferItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(TransferItem.generateGsi1Pk(merchantId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)
            .build()

        // TODO: handle pagination
        val toReturn = mutableListOf<TransferItem>()
        transferTable.index(GSI_1)
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
                    ":inc" to AttributeValue.builder().n(amount.toString()).build()
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
        feePayoutId: PayoutId? = null
    ) {
        val updatedItem = item.copy(
            data = PayoutData(
                merchantPaid = merchantPaid,
                merchantAmount = merchantAmount,
                merchantPayoutId = merchantPayoutId,
                feePaid = feePaid,
                feeAmount = feeAmount,
                feePayoutId = feePayoutId
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

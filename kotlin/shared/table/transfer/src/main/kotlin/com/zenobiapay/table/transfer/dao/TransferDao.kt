package com.zenobiapay.table.transfer.dao

import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.table.MAX_LIST_ITEMS
import com.zenobiapay.table.transfer.model.PaymentParticipantIdentity
import com.zenobiapay.model.ddb.transfer.PayoutData
import com.zenobiapay.model.ddb.transfer.PayoutId
import com.zenobiapay.model.ddb.transfer.PayoutItem
import com.zenobiapay.table.model.ContinuationToken
import com.zenobiapay.table.transfer.model.StatementItem
import com.zenobiapay.table.transfer.model.TransferData
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.TransferItem.Companion.GSI_1
import com.zenobiapay.table.transfer.model.TransferItem.Companion.GSI_2
import com.zenobiapay.table.transfer.di.TRANSFER_TABLE_NAME
import com.zenobiapay.table.transfer.model.BankAccount
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.transfer.model.Signature
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
import jakarta.inject.Inject
import jakarta.inject.Named

private val logger = KotlinLogging.logger {}

const val PAYOUT_PREFIX = "PAYOUT"

class TransferDao @Inject constructor(
    private val client: DynamoDbEnhancedClient,
    private val lowLevelClient: DynamoDbClient,
    @Named(TRANSFER_TABLE_NAME)
    private val transferTableName: String,
    private val objectMapper: ObjectMapper
) {
    private val transferTable = client.table(transferTableName, TableSchema.fromBean(TransferItem::class.java))
    private val payoutTable = client.table(transferTableName, TableSchema.fromBean(PayoutItem::class.java))

    fun putTransferRequest(
        merchantId: String,
        requestId: String,
        amountInCents: Int,
        merchantName: String,
        statementItems: List<StatementItem>,
        expiry: Long
    ) {
        val pk = TransferItem.generatePk(requestId)
        val sk = TransferItem.generateSk()
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
                ),
                ttl = expiry
            )
        )
    }

    fun updateTransferInboundStatus(
        transferItem: TransferItem,
        inboundTransferStatus: InboundTransferStatus,
    ): TransferItem {
        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(transferItem.copy(
                inboundStatus = inboundTransferStatus,
                ttl = null,
            ).also { "Updated transfer item: $it"})
            .build()

        return transferTable.updateItem(request)
    }

    fun updateTransferRequestLocked(
        transferItem: TransferItem
    ): TransferItem {
        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(transferItem.copy(
                outboundStatus = OutboundTransferStatus.FULFILL_LOCKED,
                ttl = null,
            ).also { "Updated transfer item: $it"})
            .build()

        return transferTable.updateItem(request)
    }

    fun updateTransferRequestFulfilled(
        preApproved: Boolean,
        transferItem: TransferItem,
        fulfillRequestId: String,
        customerIdentity: PaymentParticipantIdentity,
        customerBankAccount: BankAccount,
        timestamp: Instant,
        webhookUrl: String?,
        signature: Signature,
    ) {
        val updatedItem = transferItem.copy(
            inboundStatus = InboundTransferStatus.IN_FLIGHT,
            outboundStatus = if (preApproved) OutboundTransferStatus.IN_FLIGHT_APPROVED else OutboundTransferStatus.IN_FLIGHT_WAITING,
            transferFulfillId = fulfillRequestId,
            data = transferItem.data?.copy(
                customer = customerIdentity,
                webhookUrl = webhookUrl,
                signature = signature,
                customerBankAccount = customerBankAccount,
            ),
            gsi2Pk = TransferItem.generateGsi2Pk(customerIdentity.id),
            gsi2Sk = TransferItem.generateGsi2Sk(fulfillRequestId, timestamp),
            ttl = null,
        )

        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(updatedItem)
            .build()

        transferTable.updateItem(request)
    }

    fun updateTransferPayoutLocked(
        transferItem: TransferItem
    ) {
        val updatedItem = transferItem.copy(
            outboundStatus = OutboundTransferStatus.PAYOUT_LOCKED,
            ttl = null,
        )

        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(updatedItem)
            .build()

        transferTable.updateItem(request)
    }

    fun updateTransferPaidOut(
        transferItem: TransferItem,
        fee: Int?,
        orumPayoutId: String,
        version: Int,
    ) {
        val updatedItem = transferItem.copy(
            outboundStatus = OutboundTransferStatus.COMPLETED,
            data = transferItem.data?.copy(
                fee = fee,
                orumPayoutId = orumPayoutId,
            ),
            version = version,
            ttl = null,
        )

        val request = UpdateItemEnhancedRequest.builder(TransferItem::class.java)
            .item(updatedItem)
            .build()

        transferTable.updateItem(request)
    }

    fun getTransfer(transferRequestId: String): TransferItem? {
        val pk = TransferItem.generatePk(transferRequestId)
        val sk = TransferItem.generateSk()
        return try {
            transferTable.getItem {
                it.key {
                    it.partitionValue(pk).sortValue(sk)
                }
            }
        } catch (e: ResourceNotFoundException) {
            logger.error(e) { "Got ddb error during get transfer" }
            return null
        }
    }

    fun listCustomerTransfers(customerId: String, continuationToken: String?, paginationSecret: String): Pair<List<TransferItem>, ContinuationToken?> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(TransferItem.generateGsi2Pk(customerId))
        }
        val queryRequestBuilder = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)

        if (continuationToken != null) {
            logger.info { "Using continuation token $continuationToken" }
            val token = ContinuationToken.decodeToken(continuationToken, objectMapper, paginationSecret)
            queryRequestBuilder.exclusiveStartKey(token.key)
        }

        val page = transferTable.index(GSI_2)
            .query(queryRequestBuilder.build())
            .iterator()
            .asSequence()
            .firstOrNull()

        return if (page == null) {
            listOf<TransferItem>() to null
        } else {
            page.items() to page.lastEvaluatedKey()?.let { ContinuationToken(page.lastEvaluatedKey()) }
        }.also {
            logger.info { "Got ${it.first.size} items and continuation token ${it.second}" }
        }
    }

    fun listMerchantTransfers(merchantId: String, continuationToken: String?, paginationSecret: String): Pair<List<TransferItem>, ContinuationToken?> {
        logger.info { "Got table name ${transferTable.tableName()}" }
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(TransferItem.generateGsi1Pk(merchantId))
        }
        val queryRequestBuilder = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)

        if (continuationToken != null) {
            logger.info { "Using continuation token $continuationToken" }
            val token = ContinuationToken.decodeToken(continuationToken, objectMapper, paginationSecret)
            queryRequestBuilder.exclusiveStartKey(token.key)
        }

        val page = transferTable.index(GSI_1)
            .query(queryRequestBuilder.build())
            .iterator()
            .asSequence()
            .firstOrNull()

        return if (page == null) {
            listOf<TransferItem>() to null
        } else {
            page.items() to page.lastEvaluatedKey()?.let { ContinuationToken(page.lastEvaluatedKey()) }
        }.also {
            logger.info { "Got ${it.first.size} items and continuation token ${it.second}" }
        }
    }

    fun listMerchantPayouts(merchantId: String, continuationToken: String?, paginationSecret: String): Pair<List<PayoutItem>, ContinuationToken?> {
        logger.info { "Got table name ${transferTable.tableName()}" }
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(PayoutItem.generatePk(merchantId))
        }

        val queryRequestBuilder = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)

        if (continuationToken != null) {
            logger.info { "Using continuation token $continuationToken" }
            val token = ContinuationToken.decodeToken(continuationToken, objectMapper, paginationSecret)
            queryRequestBuilder.exclusiveStartKey(token.key)
        }

        val page = payoutTable.query(queryRequestBuilder.build())
            .iterator()
            .asSequence()
            .firstOrNull()

        return if (page == null) {
            listOf<PayoutItem>() to null
        } else {
            page.items() to page.lastEvaluatedKey()?.let { ContinuationToken(page.lastEvaluatedKey()) }
        }.also {
            logger.info { "Got ${it.first.size} items and continuation token ${it.second}" }
        }
    }
}
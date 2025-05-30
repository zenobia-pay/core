package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.CreateTransferRequest200Response
import com.zenobiapay.api.generated.model.CreateTransferRequestRequest
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.util.getSubForM2M
import com.zenobiapay.api.util.getUserRole
import com.zenobiapay.events.model.ItemMetadataRecord
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.StatementItem
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.transfer.di.TRANSFER_METADATA_QUEUE_URL
import com.zenobiapay.transfer.di.TRANSFER_NOTIFICATION_SECRET
import com.zenobiapay.transfer.model.TransferRequestWebsocketSignature
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.temporal.ChronoUnit
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.sqs.SqsClient
import java.util.UUID

private val logger = KotlinLogging.logger {}

class CreateTransferRequestOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val transferDao: TransferDao,
    private val userDao: UserDao,
    @Named(TRANSFER_NOTIFICATION_SECRET) val transferNotificationSecret: String,
    private val sqsClient: SqsClient,
    @Named(TRANSFER_METADATA_QUEUE_URL)
    private val transferMetadataQueueUrl: String,
): Operation<CreateTransferRequestRequest, CreateTransferRequest200Response>() {

    override val inputType = CreateTransferRequestRequest::class.java

    override fun run(request: CreateTransferRequestRequest, input: APIGatewayProxyRequestEvent, context: Context, sub: String?): CreateTransferRequest200Response {
        val expiry = Instant.now().plus(15, ChronoUnit.MINUTES).epochSecond
        val userId = when (input.requestContext.getUserRole()) {
            UserPoolGroup.MERCHANT -> sub!!
            UserPoolGroup.MERCHANT_M2M -> input.requestContext.getSubForM2M()
            UserPoolGroup.CUSTOMER, UserPoolGroup.UNKNOWN -> throw InvalidRequestException("Invalid role for transfer request")
        }
        logger.info { "Using userId = $userId" }

        val requestId = input.requestContext.requestId
        val merchantName = userDao.getUserItem(userId!!)?.data?.merchantData?.displayName
            ?: throw InvalidRequestException("Merchant display name not configured.")
        val statementItemsToMetadata = getStatementItemToMetadata(request.statementItems, request.itemMetadata)
        transferDao.putTransferRequest(
            userId,
            requestId,
            request.amount,
            merchantName,
            statementItemsToMetadata.map { it.first },
            expiry,
        )

        logger.info { "Sending sqs item to process transfer metadata" }
        val itemMetadata: Map<String, Map<String, Any>> = statementItemsToMetadata
            .filter { it.first.id != null && it.second != null }.associate { it.first.id!! to it.second!! }
        if (request.transferMetadata != null || request.itemMetadata?.isNotEmpty() == true) {
            sqsClient.sendMessage {
                it.queueUrl(transferMetadataQueueUrl)
                it.messageBody(
                    objectMapper.writeValueAsString(
                        ItemMetadataRecord(
                            merchantId = userId,
                            transferMetadata = request.transferMetadata,
                            transferRequestId = requestId,
                            itemMetadata = itemMetadata
                        )
                    )
                )
            }
        }

        return CreateTransferRequest200Response()
            .transferRequestId(requestId)
            .merchantId(userId)
            .expiry(expiry.toInt())
            .signature(
                TransferRequestWebsocketSignature(requestId, userId, expiry)
                    .toSignedHmacString(objectMapper, transferNotificationSecret)
            )
    }

    private fun getStatementItemToMetadata(
        statementItems: List<com.zenobiapay.api.generated.model.StatementItem>,
        itemMetadata: Map<String, Any>
    ): List<Pair<StatementItem, Map<String, Any>?>> {
        return statementItems.map {
            StatementItem.fromApiRequestStatementItem(it) to it.key?.let { itemMetadata[it] }
        }.map {
            val uuid = UUID.randomUUID()
            it.first.copy(id = uuid.toString()) to (it.second as? Map<String, Any>)
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT, UserPoolGroup.MERCHANT_M2M)
    }
}

package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.EditTransferRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.OrumCreateTransferRequest
import com.zenobiapay.orum.model.TransferParticipant
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.table.transfer.dao.PAYOUT_PREFIX
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.transfer.util.getFee
import com.zenobiapay.table.user.dao.UserDao
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

private data class EditTransferDetails(
    val source: TransferParticipant?,
    val destination: TransferParticipant?,
    val amount: Int,
    val onSuccess: () -> Unit
)

class EditTransferOperation @Inject constructor(
    private val transferDao: TransferDao,
    private val userDao: UserDao,
    private val orumWrapper: OrumWrapper,
): Operation<EditTransferRequest, EmptyApiResponse>() {
    override val inputType = EditTransferRequest::class.java
    
    override fun run(
        request: EditTransferRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        logger.info { "Processing edit transfer request for transfer ID: ${request.transferRequestId}" }

        val editCount = listOf(
            request.creditMerchant,
            request.debitMerchant,
            request.creditCustomer,
            request.debitCustomer,
        ).count { it }
        if (editCount > 1 || editCount == 0) {
            throw InvalidRequestException("Only one of creditMerchant, debitMerchant, creditCustomer, or debitCustomer can be set to true")
        }
        
        // Get the transfer using TransferDao
        val transfer = transferDao.getTransfer(request.transferRequestId)
            ?: throw ResourceNotFoundException("TRANSFER")
        
        // Validate that the inbound status is 'complete'
        if (transfer.inboundStatus != InboundTransferStatus.COMPLETED && transfer.inboundStatus != InboundTransferStatus.SETTLED) {
            throw InvalidRequestException("Cannot refund transfer with status: ${transfer.inboundStatus}. Transfer must be in COMPLETE status.")
        }
        
        // Prepare the refund transfer request
        val refundAmount = transfer.amount!!
        val customerIdentity = transfer.data?.customer
            ?: throw InvalidRequestException("Transfer does not have customer information")
        val merchantIdentity = transfer.data?.merchant
            ?: throw InvalidRequestException("Transfer does not have merchant information")

        val merchantPayout = transfer.amount!! - getFee(transfer.amount!!)
        logger.info { "Calculated merchant payout as $merchantPayout" }

        val customerReferenceId = generateCustomerOrumId(customerIdentity.id)
        val merchantItem = userDao.getUserItem(merchantIdentity.id) ?: throw ResourceNotFoundException("MERCHANT")
        val merchantReferenceId = merchantItem.data.orumReferenceId ?: throw ResourceNotFoundException("ORUM REFERENCE ID")
        val editTransferDetails = if (request.debitMerchant) {
            EditTransferDetails(
                source = TransferParticipant(
                    customerReferenceId = merchantReferenceId,
                    statementDisplayName = "Refund: ${customerIdentity.name ?: ""}",
                    accountReferenceId = merchantIdentity.bankAccountId
                ),
                destination = null,
                amount = merchantPayout,
            ) {
                transferDao.updateTransferOutboundStatus(
                    transferItem = transfer,
                    outboundTransferStatus = OutboundTransferStatus.REFUNDED
                )
            }
        } else if (request.creditCustomer) {
            EditTransferDetails(
                source = null,
                destination = TransferParticipant(
                    customerReferenceId = customerReferenceId,
                    statementDisplayName = "Refund: ${merchantIdentity.name ?: "Zenobia Pay"}",
                    accountReferenceId = customerIdentity.bankAccountId
                ),
                amount = refundAmount,
            ) {
                transferDao.updateTransferInboundStatus(
                    transferItem = transfer,
                    inboundTransferStatus = InboundTransferStatus.REFUNDED
                )
            }
        } else if (request.debitCustomer) {
            logger.info { "Retrying debiting customer purchase." }
            EditTransferDetails(
                source = TransferParticipant(
                    customerReferenceId = customerReferenceId,
                    statementDisplayName = merchantIdentity.name?.take(16),
                    accountReferenceId = customerIdentity.bankAccountId
                ),
                destination = null,
                amount = refundAmount,
            ) {
                transferDao.updateTransferInboundStatus(
                    transferItem = transfer,
                    inboundTransferStatus = InboundTransferStatus.IN_FLIGHT
                )
            }
        } else if (request.creditMerchant) {
            logger.info { "Locking transfer payout" }
            transferDao.updateTransferPayoutLocked(transfer)
            orumWrapper.createTransfer(
                OrumCreateTransferRequest(
                    transferReferenceId = "$PAYOUT_PREFIX#${transfer.requestId}",
                    amount = merchantPayout,
                )
            )
            EditTransferDetails(
                source = null,
                destination = TransferParticipant(
                    customerReferenceId = merchantReferenceId,
                    statementDisplayName = customerIdentity.name?.take(16),
                    accountReferenceId = customerIdentity.bankAccountId,
                ),
                amount = merchantPayout,
            ) {
                transferDao.updateTransferInboundStatus(
                    transferItem = transfer,
                    inboundTransferStatus = InboundTransferStatus.IN_FLIGHT
                )
            }
        } else throw InvalidRequestException("No refund details specified")

        val transferRequest = OrumCreateTransferRequest(
            transferReferenceId = "ADMIN_EDIT#${context.awsRequestId}",
            amount = editTransferDetails.amount,
            source = editTransferDetails.source,
            destination = editTransferDetails.destination
        )
        val response = orumWrapper.createTransfer(transferRequest)
        logger.info { "Refund transfer initiated successfully with Orum ID: ${response.transfer.id}" }
        editTransferDetails.onSuccess()

        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.ADMIN)
    }
}
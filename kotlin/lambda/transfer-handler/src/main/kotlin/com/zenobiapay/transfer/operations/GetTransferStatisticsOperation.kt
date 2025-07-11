package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetTransferStatistics200Response
import com.zenobiapay.api.generated.model.GetTransferStatisticsRequest
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.table.transfer.dao.TransferDao
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

/**
 * Operation to get aggregated transfer statistics
 */
class GetTransferStatisticsOperation @Inject constructor(
    private val transferDao: TransferDao
) : Operation<GetTransferStatisticsRequest, GetTransferStatistics200Response>() {
    
    override val inputType = GetTransferStatisticsRequest::class.java
    
    override fun run(
        request: GetTransferStatisticsRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): GetTransferStatistics200Response {
        logger.info { "Getting transfer statistics" }
        
        userId!!

        val startInstant = Instant.parse(request.startTime)
        val endInstant = Instant.parse(request.endTime)
        val statistics = transferDao.getTransferStatistics(userId, startInstant, endInstant)
        
        logger.info { "Successfully retrieved transfer statistics" }
        return GetTransferStatistics200Response()
            .amountPaid(statistics.amountPaid)
            .amountSettled(statistics.amountSettled)
            .fee(statistics.fee)
            .transferCount(statistics.transferCount)
    }
    
    /**
     * Parse a date string in ISO format (YYYY-MM-DD)
     */
    private fun parseDate(dateString: String): LocalDate {
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
        } catch (e: DateTimeParseException) {
            throw InvalidRequestException("Invalid date format. Expected format: YYYY-MM-DD")
        }
    }
    
    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}

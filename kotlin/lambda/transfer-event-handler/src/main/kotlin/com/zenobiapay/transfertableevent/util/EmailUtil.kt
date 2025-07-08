package com.zenobiapay.transfertableevent.util

import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.transfertableevent.di.SENDER_EMAIL
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.*
import java.util.*

private val logger = KotlinLogging.logger {}

class EmailUtil @Inject constructor(
    private val sesClient: SesClient,
    @Named(SENDER_EMAIL) private val senderEmail: String
) {
    /**
     * Sends a formatted email notification about a transfer status change
     * 
     * @param recipientEmail The email address to send the notification to
     * @param merchantName The name of the merchant receiving the notification
     * @param customerName The name of the customer who made the payment
     * @param requestId The transfer request ID
     * @param status The current status of the transfer
     */
    fun sendEmail(
        recipientEmail: String,
        merchantName: String,
        customerName: String,
        requestId: String,
        status: OutboundTransferStatus
    ) {
        // Only send emails for specific statuses
        if (status != OutboundTransferStatus.IN_FLIGHT_WAITING && 
            status != OutboundTransferStatus.IN_FLIGHT_APPROVED && 
            status != OutboundTransferStatus.COMPLETED &&
            status != OutboundTransferStatus.FAILED) {
            logger.info { "Skipping email for status $status as it's not configured for email notifications" }
            return
        }
        
        try {
            logger.info { "Sending email notification to $recipientEmail for transfer $requestId with status $status" }
            
            val statusText = getStatusText(status)
            if (statusText == null) {
                logger.warn { "No status text mapping for $status. Skipping email." }
                return
            }
            
            val subject = "Payment $statusText - Transfer ID: $requestId"
            val htmlBody = buildEmailBody(merchantName, customerName, requestId, status)
            
            val request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(Destination.builder().toAddresses(recipientEmail).build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).charset("UTF-8").build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                        .build())
                    .build())
                .build()
            
            val response = sesClient.sendEmail(request)
            logger.info { "Email sent successfully with message ID: ${response.messageId()}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send email notification to $recipientEmail" }
            // We don't want to fail the entire process if email sending fails
            // So we just log the error and continue
        }
    }
    
    private fun getStatusText(status: OutboundTransferStatus): String? {
        return when (status) {
            OutboundTransferStatus.COMPLETED -> "Funds sent"
            OutboundTransferStatus.IN_FLIGHT_WAITING -> "Approved"
            OutboundTransferStatus.IN_FLIGHT_APPROVED -> "Approved"
            OutboundTransferStatus.FAILED -> "Failed"
            else -> null
        }
    }
    
    private fun buildEmailBody(
        merchantName: String,
        customerName: String,
        requestId: String,
        status: OutboundTransferStatus
    ): String {
        val statusColor = when (status) {
            OutboundTransferStatus.COMPLETED -> "#28a745" // Green
            OutboundTransferStatus.IN_FLIGHT_WAITING -> "#28a745" // Green
            OutboundTransferStatus.IN_FLIGHT_APPROVED -> "#28a745" // Green
            OutboundTransferStatus.FAILED -> "#dc3545" // Red
            else -> "#6c757d" // Gray (should not happen due to filtering)
        }
        
        val statusText = getStatusText(status)
        val statusMessage = when (status) {
            OutboundTransferStatus.COMPLETED -> "The payment has been completed and funds are available."
            OutboundTransferStatus.IN_FLIGHT_WAITING -> "The payment is approved."
            OutboundTransferStatus.IN_FLIGHT_APPROVED -> "The payment is being processed. Please wait for confirmation before fulfilling the order."
            OutboundTransferStatus.FAILED -> "The payment has been rejected. Ensure the customer has enough funds and has a bank account in good standing."
            else -> "The payment status has been updated." // Should not happen due to filtering
        }
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Notification</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { text-align: center; padding: 20px 0; }
                    .logo { max-width: 150px; }
                    .content { background-color: #f9f9f9; border-radius: 5px; padding: 20px; margin-bottom: 20px; }
                    .status { display: inline-block; padding: 8px 16px; border-radius: 4px; font-weight: bold; color: white; background-color: ${statusColor}; }
                    .details { margin: 20px 0; }
                    .details-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }
                    .footer { font-size: 12px; text-align: center; color: #777; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Payment Notification</h1>
                </div>
                <div class="content">
                    <p>Hello $merchantName,</p>
                    <p>This is a notification about a payment from $customerName.</p>
                    <p>Payment Status: <span class="status">$statusText</span></p>
                    <p>$statusMessage</p>
                    
                    <div class="details">
                        <h3>Payment Details:</h3>
                        <div class="details-row">
                            <span>Customer:</span>
                            <span>$customerName</span>
                        </div>
                        <div class="details-row">
                            <span>Transfer ID:</span>
                            <span>$requestId</span>
                        </div>
                        <div class="details-row">
                            <span>Status:</span>
                            <span>$statusText</span>
                        </div>
                    </div>
                    
                    <p>You can view more details about this transaction in your Zenobia Pay dashboard.</p>
                </div>
                <div class="footer">
                    <p>This is an automated message from Zenobia Pay. Please do not reply to this email.</p>
                    <p>&copy; ${Calendar.getInstance().get(Calendar.YEAR)} Zenobia Pay. All rights reserved.</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
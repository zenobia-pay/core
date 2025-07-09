package com.zenobiapay.transfertableevent.logic

import com.zenobia.metric.MetricHelper
import com.zenobiapay.api.generated.model.TransferStatus
import com.zenobiapay.table.transfer.model.InboundTransferStatus
import com.zenobiapay.table.transfer.model.OutboundTransferStatus
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.transfertableevent.util.EmailUtil
import com.zenobiapay.transfertableevent.util.WebhookUtil
import com.zenobiapay.transfertableevent.util.WebsocketUtil
import com.zenobiapay.webhook.util.isValidWebhook
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import okhttp3.Response
import org.junit.jupiter.api.Test

class TransferTableEventLogicTest {
    val webhookUtil = mockk<WebhookUtil>(relaxed = true)
    val websocketUtil = mockk<WebsocketUtil>(relaxed = true)
    val metricsHelper = mockk<MetricHelper>(relaxed = true)
    val userDao = mockk<UserDao>(relaxed = true)
    val emailUtil = mockk<EmailUtil>(relaxed = true)

    @Test
    fun `test send event when payment completes`() {
        mockWebhookSuccess()
        val logic = TransferTableEventLogic(webhookUtil, websocketUtil, metricsHelper, userDao, emailUtil)
        val oldImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.IN_FLIGHT_APPROVED)
        val newImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.COMPLETED)
        logic.handleRecord(
            oldImage,
            newImage
        )

        verify {
            websocketUtil.sendWebsocketUpdate(any(), any(), TransferStatus.SETTLED, any())
        }
    }

    @Test
    fun `test send event when payment is in flight`() {
        mockWebhookSuccess()
        val logic = TransferTableEventLogic(webhookUtil, websocketUtil, metricsHelper, userDao, emailUtil)
        val oldImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.FULFILL_LOCKED)
        val newImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.IN_FLIGHT_WAITING)
        logic.handleRecord(
            oldImage,
            newImage
        )

        verify {
            websocketUtil.sendWebsocketUpdate(any(), any(), TransferStatus.PAID, any())
        }
    }

    @Test
    fun `test send event when payment fails and was waiting`() {
        mockWebhookSuccess()
        val logic = TransferTableEventLogic(webhookUtil, websocketUtil, metricsHelper, userDao, emailUtil)
        val oldImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.IN_FLIGHT_WAITING)
        val newImage = createTransferItem(InboundTransferStatus.FAILED, OutboundTransferStatus.IN_FLIGHT_WAITING)
        logic.handleRecord(
            oldImage,
            newImage
        )

        verify {
            websocketUtil.sendWebsocketUpdate(any(), any(), any(), any())
        }
    }

    @Test
    fun `test doesnt send event when payment already complete but inbound failed`() {
        mockWebhookSuccess()
        val logic = TransferTableEventLogic(webhookUtil, websocketUtil, metricsHelper, userDao, emailUtil)
        val oldImage = createTransferItem(InboundTransferStatus.IN_FLIGHT, OutboundTransferStatus.COMPLETED)
        val newImage = createTransferItem(InboundTransferStatus.FAILED, OutboundTransferStatus.COMPLETED)
        logic.handleRecord(
            oldImage,
            newImage
        )

        verify(exactly = 0) {
            websocketUtil.sendWebsocketUpdate(any(), any(), any(), any())
        }
    }

    @Test
    fun `test doesnt send event when no status changed`() {
        mockWebhookSuccess()
        val logic = TransferTableEventLogic(webhookUtil, websocketUtil, metricsHelper, userDao, emailUtil)
        val oldImage = createTransferItem(InboundTransferStatus.FAILED, OutboundTransferStatus.IN_FLIGHT_WAITING)
        val newImage = createTransferItem(InboundTransferStatus.FAILED, OutboundTransferStatus.IN_FLIGHT_WAITING)
        logic.handleRecord(
            oldImage,
            newImage
        )

        verify(exactly = 0) {
            websocketUtil.sendWebsocketUpdate(any(), any(), TransferStatus.FAILED, any())
        }
    }

    private fun mockWebhookSuccess() {
        val successResponse = mockk<Response>(relaxed = true)
        mockkStatic("com.zenobiapay.webhook.util.WebhookValidatorKt")
        every {
            isValidWebhook(any())
        } returns true
        every {
            successResponse.isSuccessful
        } returns true
        every {
            webhookUtil.sendTransferStatus(any(), any(), any(), any(), any(), any())
        } returns successResponse
    }

    private fun createTransferItem(
        inboundStatus: InboundTransferStatus,
        outboundStatus: OutboundTransferStatus,
    ): TransferItem {
        val mockTransferItem = mockk<TransferItem>(relaxed = true)
        every {
            mockTransferItem.inboundStatus
        } returns inboundStatus
        every {
            mockTransferItem.outboundStatus
        } returns outboundStatus
        return mockTransferItem
    }
}
package com.zenobiapay.model.dao

import com.zenobiapay.dao.TransferDao
import com.zenobiapay.model.ddb.transfer.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.enhanced.dynamodb.mapper.BeanTableSchema
import java.time.Instant

class TransferDaoTest {
    private val mockLowLevelClient = mockk<DynamoDbClient>()
    private val mockEnhancedClient = mockk<DynamoDbEnhancedClient>()
    private val mockTransferRequestTable = mockk<DynamoDbTable<TransferItem>>()
    private val mockTransferPayoutTable = mockk<DynamoDbTable<PayoutItem>>(relaxed = true)
    private val tableName = "tableName"
    private val dao = TransferDao(mockEnhancedClient, mockLowLevelClient, tableName)

    @BeforeEach
    fun setup() {
        val requestSchema = BeanTableSchema.create(TransferItem::class.java)
        val payoutSchema = BeanTableSchema.create(PayoutItem::class.java)
    }

    // @Test TODO: enable once mocking is working
    fun `test updateTransferRequest updates secondary indices`() {
        every {
            mockEnhancedClient.table(tableName, any<TableSchema<TransferItem>>())
        } returns mockTransferRequestTable
        every {
            mockEnhancedClient.table(tableName, any<TableSchema<PayoutItem>>())
        } returns mockTransferPayoutTable

        val oldItem = TransferItem(
            pk = "pk",
            sk = "sk",
            gsi1Pk = "gsi1Pk",
            gsi1Sk = "gsi1Sk",
            amount = 900,
            status = TransferStatus.NOT_STARTED,
            data = TransferData(
                statementItems = listOf(
                    StatementItem(
                        "item1",
                        10,
                    )
                ),
                merchant = PaymentParticipantIdentity(
                    id = "merchantId",
                    name = "merchantName",
                    bankAccountId = "accountId",
                )
            )

        )
        val fulfillRequestId = "fulfillRequestId"
        val customerId = "customerId"
        val customerName = "customerName"
        val timestamp = Instant.now()

        val newDdbItemSlot = slot<TransferItem>()
        every {
            mockTransferRequestTable.updateItem(capture(newDdbItemSlot))
        } returns oldItem

        dao.updateTransferRequest(
            oldItem,
            fulfillRequestId = fulfillRequestId,
            customerIdentity = PaymentParticipantIdentity(
                customerId,
                customerName
            ),
            timestamp,
        )

        assertTrue(newDdbItemSlot.isCaptured)
        val newItem = newDdbItemSlot.captured
        assertEquals(newItem.gsi2Pk, "FULFILL#c_$customerId")
        assertEquals(newItem.gsi2Sk, fulfillRequestId)
        assertEquals(newItem.gsi3Pk, "FULFILL#c_$customerId")
        assertEquals(newItem.gsi3Sk, "CREATED#t_$timestamp#id_$fulfillRequestId")
    }
}
package com.zenobiapay.table.transfer.dao

//import com.zenobiapay.model.ddb.transfer.PaymentParticipantIdentity
//import com.zenobiapay.model.ddb.transfer.PayoutItem
//import com.zenobiapay.model.ddb.transfer.StatementItem
//import com.zenobiapay.model.ddb.transfer.TransferData
//import com.zenobiapay.model.ddb.transfer.TransferItem
//import com.zenobiapay.table.transfer.model.TransferStatus
//import io.mockk.MockKMatcherScope
//import io.mockk.every
//import io.mockk.mockk
//import io.mockk.slot
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertTrue
//import org.junit.jupiter.api.BeforeEach
//import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
//import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
//import software.amazon.awssdk.enhanced.dynamodb.TableSchema
//import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient
//import java.time.Instant
//import kotlin.test.Test
//
//class TransferDaoTest {
//    private val mockLowLevelClient = mockk<DynamoDbClient>()
//    private val mockEnhancedClient = mockk<DynamoDbEnhancedClient>()
//    private val mockTransferRequestTable = mockk<DynamoDbTable<TransferItem>>()
//    private val mockTransferPayoutTable = mockk<DynamoDbTable<PayoutItem>>()
//    private val tableName = "tableName"
//
//    @BeforeEach
//    fun setup() {
//        every {
//            mockEnhancedClient.table(tableName, TableSchema.fromBean(TransferItem::class.java))
//        } returns mockTransferRequestTable
//        every {
//            mockEnhancedClient.table(tableName, TableSchema.fromBean(PayoutItem::class.java))
//        } returns mockTransferPayoutTable
//    }
//
//    @Test
//    fun `test updateTransferRequest updates secondary indices`() {
//        val oldItem = TransferItem(
//            pk = "pk",
//            sk = "sk",
//            gsi1Pk = "gsi1Pk",
//            gsi1Sk = "gsi1Sk",
//            amount = 900,
//            status = TransferStatus.NOT_STARTED,
//            data = TransferData(
//                statementItems = listOf(
//                    StatementItem(
//                        "item1",
//                        10
//                    )
//                ),
//                merchant = PaymentParticipantIdentity(
//                    id = "merchantId",
//                    name = "merchantName",
//                    bankAccountId = "accountId"
//                )
//            )
//
//        )
//        val fulfillRequestId = "fulfillRequestId"
//        val customerId = "customerId"
//        val customerName = "customerName"
//        val timestamp = Instant.now()
//
//        val newDdbItemSlot = slot<UpdateItemEnhancedRequest<TransferItem>>()
//        every {
//            mockTransferRequestTable.updateItem(MockKMatcherScope.capture(newDdbItemSlot))
//        } returns oldItem
//
//        val dao = TransferDao(mockEnhancedClient, mockLowLevelClient, tableName)
//        dao.updateTransferRequest(
//            oldItem,
//            fulfillRequestId = fulfillRequestId,
//            customerIdentity = PaymentParticipantIdentity(
//                customerId,
//                customerName
//            ),
//            timestamp,
//            null
//        )
//
//        assertTrue(newDdbItemSlot.isCaptured)
//        val newItem = newDdbItemSlot.captured.item()
//        assertEquals("TRANSFER#c_$customerId", newItem.gsi2Pk)
//        assertEquals(fulfillRequestId, newItem.gsi2Sk)
//        assertEquals(newItem.gsi3Pk, "TRANSFER#c_$customerId")
//        assertEquals("CREATED#t_$timestamp#id_$fulfillRequestId", newItem.gsi3Sk)
//    }
//}

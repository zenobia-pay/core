package com.zenobiapay.transfer.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.api.model.exception.InvalidSignatureException
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.generated.model.CertificateType
import com.zenobiapay.api.generated.model.FulfillTransferRequest
import com.zenobiapay.api.generated.model.FulfillTransferRequestSignature
import com.zenobiapay.api.generated.model.SignatureType
import com.zenobiapay.cryptography.util.isSignatureValid
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.table.bank.model.BankAccountItem
import com.zenobiapay.table.bank.model.BankData
import com.zenobiapay.table.bank.model.BankPermissions
import com.zenobiapay.table.bank.model.DeviceCertificate
import com.zenobiapay.table.transfer.dao.TransferDao
import com.zenobiapay.table.transfer.model.PaymentParticipantIdentity
import com.zenobiapay.table.transfer.model.TransferData
import com.zenobiapay.table.transfer.model.TransferItem
import com.zenobiapay.table.transfer.model.TransferStatus
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.table.user.model.UserItem
import com.zenobiapay.table.user.model.UserItemData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FulfillTransferOperationTest {
    private val orumWrapper = mockk<OrumWrapper>()
    private val transferDao = mockk<TransferDao>()
    private val bankDao = mockk<BankDao>()
    private val userDao = mockk<UserDao>()
    private val objectMapper = jacksonObjectMapper()
    private val context = mockk<Context>()

    companion object {
        private val TRANSFER_REQUEST_ID = "transferRequestId"
        private val MERCHANT_ID = "merchantId"
        private val BANK_ACCOUNT_ID = "bankAccountId"
        private val DEVICE_ID = "deviceId"
        private val SIGNATURE_TYPE = SignatureType.SHA256_WITH_ECDSA
        private val SIGNATURE = "signature"
        private val USER_ID = "userId"
        private val CERT_VALUE = "certValue"
        private val REQUEST_ID = "requestId"
    }

    @Test
    fun `test validate request signature returns failure`() {
        every {
            transferDao.getMerchantTransfer(MERCHANT_ID, TRANSFER_REQUEST_ID)
        } returns createTransferItem(100, TransferStatus.NOT_STARTED)
        every {
            bankDao.getBankAccount(USER_ID, DEVICE_ID, BANK_ACCOUNT_ID)
        } returns createBankAccountItem(BankPermissions.SEND_ONLY, DeviceCertificate(CertificateType.EC.value, CERT_VALUE))
        every {
            userDao.getUserItem(MERCHANT_ID)
        } returns createUserItem()
        mockkStatic("com.zenobiapay.cryptography.util.CryptographyUtilKt")
        every {
            isSignatureValid(any(), CERT_VALUE, SIGNATURE, SIGNATURE_TYPE)
        } returns false
        val operation = FulfillTransferOperation(
            orumWrapper,
            transferDao,
            bankDao,
            userDao,
            objectMapper,
        )
        assertThrows<InvalidSignatureException> {
            operation.run(createMockGatewayEvent(), context, USER_ID)
        }
    }

    @Test
    fun `throws error on merchant transfer not existing`() {
        every {
            transferDao.getMerchantTransfer(MERCHANT_ID, TRANSFER_REQUEST_ID)
        } returns null
        val operation = FulfillTransferOperation(
            orumWrapper,
            transferDao,
            bankDao,
            userDao,
            objectMapper,
        )
        assertThrows<ResourceNotFoundException> {
            operation.run(createMockGatewayEvent(), context, USER_ID)
        }
    }

    private fun createMockGatewayEvent(): APIGatewayProxyRequestEvent {
        val mockEvent = mockk<APIGatewayProxyRequestEvent>()
        every {
            mockEvent.body
        } returns objectMapper.writeValueAsString(createRequest())
        return mockEvent
    }

    private fun createRequest(): FulfillTransferRequest {
        return FulfillTransferRequest()
            .transferRequestId(TRANSFER_REQUEST_ID)
            .merchantId(MERCHANT_ID)
            .bankAccountId(BANK_ACCOUNT_ID)
            .deviceId(DEVICE_ID)
            .signature(FulfillTransferRequestSignature()
                .signatureType(SIGNATURE_TYPE)
                .signatureValue(SIGNATURE))
    }

    private fun createTransferItem(
        amount: Int,
        status: TransferStatus
    ): TransferItem {
        return TransferItem(
            amount = amount,
            status = status,
            data = TransferData(
                merchant = PaymentParticipantIdentity("id", "name")
            )
        )
    }

    private fun createBankAccountItem(
        bankPermissions: BankPermissions,
        deviceCertificate: DeviceCertificate,
    ): BankAccountItem {
        return BankAccountItem(
            data = BankData(
                bankAccountId = BANK_ACCOUNT_ID,
                bankPermissions = bankPermissions,
                deviceCertificate = deviceCertificate,
            )
        )
    }

    private fun createUserItem(): UserItem {
        return UserItem(
            data = UserItemData(
                merchantData = MerchantData(
                    webhookUrl = "webhookUrl"
                )
            )
        )
    }
}
package com.zenobiapay.table.bank.dao

import com.zenobiapay.table.MAX_LIST_ITEMS
import com.zenobiapay.table.bank.model.BankAccountItem
import com.zenobiapay.table.bank.model.BankData
import com.zenobiapay.table.bank.model.BankPermissions
import com.zenobiapay.table.bank.model.DeviceCertificate
import com.zenobiapay.table.di.BANK_TABLE_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class BankDao @Inject constructor(
    private val enhancedClient: DynamoDbEnhancedClient,
    @Named(BANK_TABLE_NAME) private val bankTableName: String
) {
    fun putBankAccount(
        userId: String,
        deviceId: String?,
        token: String,
        plaidItemId: String,
        bankAccountId: String,
        bankAccountName: String,
        bankAccountType: String,
        orumId: String,
        deviceCertificate: DeviceCertificate?
    ) {
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        val pk = BankAccountItem.generatePk(userId, deviceId)
        val sk = BankAccountItem.generateSk(bankAccountId)

        val bankPermissions = if (deviceId == null) {
            logger.info { "Setting bank as receive only, no device id found" }
            BankPermissions.RECEIVE_ONLY
        } else {
            logger.info { "Found device id. Setting bank as send only"}
            BankPermissions.SEND_ONLY
        }

        table.putItem(
            BankAccountItem(
                pk = pk,
                sk = sk,
                publicToken = token,
                data = BankData(
                    bankAccountId = bankAccountId,
                    bankAccountName = bankAccountName,
                    bankAccountType = bankAccountType,
                    orumId = orumId,
                    plaidItemId = plaidItemId,
                    bankPermissions = bankPermissions,
                    deviceCertificate = deviceCertificate
                )
            )
        )
    }

    // TODO: handle paging using continuation token
    fun listBankAccounts(userId: String, deviceId: String?, continuationToken: String?): List<BankAccountItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(BankAccountItem.generatePk(userId, deviceId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .attributesToProject("pk", "sk", "data")
            .queryConditional(queryConditional)
            .limit(MAX_LIST_ITEMS)
            .build()

        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        return table.query(queryRequest).items().toList()
    }

    fun getBankAccount(userId: String, deviceId: String?, bankAccountId: String): BankAccountItem? {
        logger.info { "Fetch bank account from userId $userId, bankAccountId $bankAccountId" }
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        val pk = BankAccountItem.generatePk(userId, deviceId)
        val sk = BankAccountItem.generateSk(bankAccountId)

        return try {
            table.getItem {
                it.key {
                    it.partitionValue(pk).sortValue(sk)
                }
            }
        } catch (e: ResourceNotFoundException) {
            null
        }
    }
}
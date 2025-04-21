package com.zenobiapay.table.bank.dao

import com.zenobiapay.table.MAX_LIST_ITEMS
import com.zenobiapay.table.bank.model.BankAccountItem
import com.zenobiapay.table.bank.model.BankData
import com.zenobiapay.table.bank.model.BankPermissions
import com.zenobiapay.table.bank.model.DeviceCertificate
import com.zenobiapay.table.di.BANK_TABLE_NAME
import com.zenobiapay.table.model.ContinuationToken
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class BankDao @Inject constructor(
    private val enhancedClient: DynamoDbEnhancedClient,
    @Named(BANK_TABLE_NAME) private val bankTableName: String
) {
    val bankTable = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
    fun putBankAccount(
        userId: String,
        deviceId: String?,
        lastFourDigits: String,
        token: String,
        plaidItemId: String,
        bankAccountId: String,
        bankAccountName: String,
        bankAccountType: String,
        orumId: String,
        deviceCertificate: DeviceCertificate?
    ) {
        val pk = BankAccountItem.generatePk(userId, deviceId)
        val sk = BankAccountItem.generateSk(bankAccountId)

        val bankPermissions = if (deviceId == null) {
            logger.info { "Setting bank as receive only, no device id found" }
            BankPermissions.RECEIVE_ONLY
        } else {
            logger.info { "Found device id. Setting bank as send only"}
            BankPermissions.SEND_ONLY
        }

        bankTable.putItem(
            BankAccountItem(
                pk = pk,
                sk = sk,
                accessToken = token,
                data = BankData(
                    bankAccountId = bankAccountId,
                    bankAccountName = bankAccountName,
                    lastFourDigits = lastFourDigits,
                    bankAccountType = bankAccountType,
                    orumId = orumId,
                    plaidItemId = plaidItemId,
                    bankPermissions = bankPermissions,
                    deviceCertificate = deviceCertificate
                )
            )
        )
    }

    fun listBankAccounts(userId: String, deviceId: String?, continuationToken: ContinuationToken?): Pair<List<BankAccountItem>, ContinuationToken?> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(BankAccountItem.generatePk(userId, deviceId))
        }
        val queryRequestBuilder = QueryEnhancedRequest.builder()
            .attributesToProject("pk", "sk", "data")
            .queryConditional(queryConditional)
            .limit(MAX_LIST_ITEMS)

        if (continuationToken != null) {
            logger.info { "Using continuation token $continuationToken" }
            queryRequestBuilder.exclusiveStartKey(continuationToken.key)
        }

        val page = bankTable.query(queryRequestBuilder.build())
            .iterator()
            .asSequence()
            .firstOrNull()

        return if (page == null) {
            listOf<BankAccountItem>() to null
        } else {
            page.items() to page.lastEvaluatedKey()?.let { ContinuationToken(page.lastEvaluatedKey()) }
        }.also {
            logger.info { "Got ${it.first.size} items and continuation token ${it.second}" }
        }
    }

    fun getBankAccount(userId: String, bankAccountId: String, deviceId: String?): BankAccountItem {
        logger.info { "Fetch bank account from userId $userId, bankAccountId $bankAccountId" }
        val pk = BankAccountItem.generatePk(userId, deviceId)
        val sk = BankAccountItem.generateSk(bankAccountId)

        return bankTable.getItem {
            it.key {
                it.partitionValue(pk).sortValue(sk)
            }
        }
    }

    fun deleteBankAccount(userId: String, bankAccountId: String, deviceId: String?) {
        logger.info { "Deleting bank account for user $userId, bankAccountId $bankAccountId, deviceId $deviceId" }
        val item = getBankAccount(userId, bankAccountId, deviceId)
        val deletedItem = item.copy(
            pk = BankAccountItem.generateDeletedPk(userId, deviceId)
        )
        logger.info { "Updating ddb to use new partition value ${deletedItem.pk}" }
        enhancedClient.transactWriteItems { builder ->
            builder.addDeleteItem(bankTable, TransactDeleteItemEnhancedRequest.builder()
                .key { it.partitionValue(item.pk).sortValue(item.sk) }
                .build()
            )
            builder.addPutItem(bankTable, deletedItem)
        }
    }
}
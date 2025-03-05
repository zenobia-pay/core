package com.zenobiapay.dao

import com.zenobiapay.di.BANK_TABLE_NAME
import com.zenobiapay.model.ddb.bank.BankData
import com.zenobiapay.model.ddb.bank.BankAccountItem
import com.zenobiapay.util.MAX_BANK_ITEMS
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
        plaidItemId: String,
        bankAccountId: String,
        bankAccountName: String,
        token: String,
        bankAccountType: String,
        orumId: String,
    ) {
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        val pk = BankAccountItem.generatePk(userId)
        val sk = BankAccountItem.generateSk(bankAccountId)
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
                )
            )
        )
    }

    // TODO: handle paging using continuation token
    fun listBankAccounts(userId: String, continuationToken: String?): List<BankAccountItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(BankAccountItem.generatePk(userId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .attributesToProject("pk", "sk", "data")
            .queryConditional(queryConditional)
            .limit(MAX_BANK_ITEMS)
            .build()

        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        return table.query(queryRequest).items().toList()
    }

    fun getBankAccount(userId: String, bankAccountId: String): BankAccountItem? {
        logger.info { "Fetch bank account from userId $userId, bankAccountId $bankAccountId" }
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankAccountItem::class.java))
        val pk = BankAccountItem.generatePk(userId)
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
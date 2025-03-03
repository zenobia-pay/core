package com.zenobiapay.dao

import com.zenobiapay.di.BANK_TABLE_NAME
import com.zenobiapay.model.ddb.bank.BankData
import com.zenobiapay.model.ddb.bank.BankItem
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
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankItem::class.java))
        val pk = BankItem.generatePk(userId)
        val sk = BankItem.generateSk(bankAccountId)
        table.putItem(
            BankItem(
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
    fun listBankAccounts(userId: String, continuationToken: String?): List<BankItem> {
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(BankItem.generatePk(userId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .attributesToProject("pk", "sk", "data")
            .queryConditional(queryConditional)
            .limit(MAX_BANK_ITEMS)
            .build()

        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankItem::class.java))
        return table.query(queryRequest).items().toList()
    }

    fun getBankAccount(userId: String, bankAccountId: String): BankItem {
        logger.info { "Fetch bank account from userId $userId, bankAccountId $bankAccountId" }
        val table = enhancedClient.table(bankTableName, TableSchema.fromBean(BankItem::class.java))
        val pk = BankItem.generatePk(userId)
        val sk = BankItem.generateSk(bankAccountId)

        try {
            return table.getItem {
                it.key {
                    it.partitionValue(pk).sortValue(sk)
                }
            }
        } catch (e: ResourceNotFoundException) {
            logger.error(e) { "Failed to fetch bank item from userId $userId, accountId $bankAccountId" }
            throw com.zenobiapay.model.exception.ResourceNotFoundException("ACCOUNT")
        }
    }
}
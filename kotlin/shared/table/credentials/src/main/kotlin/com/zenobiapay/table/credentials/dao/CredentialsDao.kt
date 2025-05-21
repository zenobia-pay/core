package com.zenobiapay.table.credentials.dao

import com.zenobiapay.table.credentials.model.BankHashMappingTableItem
import com.zenobiapay.table.credentials.model.CredentialsTableItem
import com.zenobiapay.table.di.CREDENTIALS_TABLE_NAME
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import java.security.SecureRandom
import java.util.Base64
import jakarta.inject.Inject
import jakarta.inject.Named

const val BYTE_LENGTH = 64
const val REFRESH_TOKEN_HASHING_SECRET = "REFRESH_TOKEN_HASHING_SECRET"

private val logger = KotlinLogging.logger {}

class CredentialsDao @Inject constructor(
    private val enhancedClient: DynamoDbEnhancedClient,
    @Named(CREDENTIALS_TABLE_NAME) private val credentialsTableName: String,
    @Named(REFRESH_TOKEN_HASHING_SECRET) private val refreshTokenHashingSecret: String
) {
    private val credentialsTable: DynamoDbTable<CredentialsTableItem> = enhancedClient.table(credentialsTableName, TableSchema.fromBean(CredentialsTableItem::class.java))
    private val bankHashMappingTable: DynamoDbTable<BankHashMappingTableItem> = enhancedClient.table(credentialsTableName, TableSchema.fromBean(BankHashMappingTableItem::class.java))

    fun createRefreshToken(sub: String, exchangeRequestId: String, userAgent: String): String {
        val refreshToken = generateRefreshToken()
        credentialsTable.putItem(
            CredentialsTableItem(
                pk = sub,
                sk = CredentialsTableItem.hashRefreshToken(refreshToken, refreshTokenHashingSecret),
                exchangeRequestId = exchangeRequestId,
                userAgent = userAgent
            )
        )
        return refreshToken
    }

    private fun generateRefreshToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(BYTE_LENGTH)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun putBankHash(sub: String, bankHash: String) {
        bankHashMappingTable.updateItem(
            BankHashMappingTableItem(
                pk = BankHashMappingTableItem.generatePk(bankHash),
                sk = BankHashMappingTableItem.generateSk(),
                sub = sub
            )
        )
    }

    fun deleteRefreshToken(sub: String, refreshToken: String) {
        val hashedRefreshToken = CredentialsTableItem.hashRefreshToken(refreshToken, refreshTokenHashingSecret)
        logger.info { "Attempting to delete refresh token for sub: $sub" }
        
        credentialsTable.deleteItem { 
            it.key { 
                it.partitionValue(sub)
                it.sortValue(hashedRefreshToken)
            }
        }
        logger.info { "Successfully deleted refresh token for sub: $sub" }
    }

    fun getSubByBankHash(bankHash: String): String? {
        val pk = BankHashMappingTableItem.generatePk(bankHash)
        val sk = BankHashMappingTableItem.generateSk()
        return bankHashMappingTable.getItem(
            BankHashMappingTableItem(
                pk = pk,
                sk = sk
            )
        )?.sub
    }

}
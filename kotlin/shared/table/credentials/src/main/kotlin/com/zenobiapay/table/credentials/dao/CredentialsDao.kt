package com.zenobiapay.table.credentials.dao

import com.zenobiapay.table.credentials.model.CredentialsTableItem
import com.zenobiapay.table.di.CREDENTIALS_TABLE_NAME
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named

const val BYTE_LENGTH = 64
const val REFRESH_TOKEN_HASHING_SECRET = "REFRESH_TOKEN_HASHING_SECRET"
class CredentialsDao @Inject constructor(
    private val enhancedClient: DynamoDbEnhancedClient,
    @Named(CREDENTIALS_TABLE_NAME) private val credentialsTableName: String,
    @Named(REFRESH_TOKEN_HASHING_SECRET) private val refreshTokenHashingSecret: String
) {
    val credentialsTable: DynamoDbTable<CredentialsTableItem> = enhancedClient.table(credentialsTableName, TableSchema.fromBean(CredentialsTableItem::class.java))

    fun createRefreshToken(sub: String): String {
        val refreshToken = generateRefreshToken()
        credentialsTable.putItem(
            CredentialsTableItem(
                pk = sub,
                hashedRefreshToken = CredentialsTableItem.hashRefreshToken(refreshToken, refreshTokenHashingSecret)
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

}
package com.zenobiapay.dao

import com.zenobiapay.di.CREDENTIALS_TABLE_NAME
import com.zenobiapay.model.ddb.credentials.CredentialsItem
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import javax.inject.Inject
import javax.inject.Named

class CredentialsDao @Inject constructor(
    enhancedClient: DynamoDbEnhancedClient,
    @Named(CREDENTIALS_TABLE_NAME) private val credentialsTableName: String
) {
    private val credentialsTable = enhancedClient.table(credentialsTableName, TableSchema.fromBean(CredentialsItem::class.java))

    fun putCredentials(clientName: String, clientId: String, userId: String) {
        credentialsTable.putItem(
            CredentialsItem(
                pk = clientId,
                clientName = clientName,
                sub = userId,
            )
        )
    }

    fun getCredentials(clientId: String): CredentialsItem {
        return credentialsTable.getItem(
            Key.builder().partitionValue(clientId).build()
        )
    }
}
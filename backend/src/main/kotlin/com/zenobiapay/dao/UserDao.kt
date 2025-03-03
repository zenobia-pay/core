package com.zenobiapay.dao

import com.zenobiapay.di.USER_TABLE_NAME
import com.zenobiapay.model.api.account.Location
import com.zenobiapay.model.ddb.user.*
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class UserDao @Inject constructor(
    private val client: DynamoDbEnhancedClient,
    @Named(USER_TABLE_NAME) private val userTableName: String
) {
    private val userTable = client.table(userTableName, TableSchema.fromBean(UserItem::class.java))
    private val merchantTable = client.table(userTableName, TableSchema.fromBean(MerchantItem::class.java))

    fun putUser(sub: String, orumPersonId: String) {
        userTable.putItem(
            UserItem(
                pk = UserItem.generatePk(sub),
                sk = UserItem.generateSk(),
                data = UserItemData(
                    orumPersonId = orumPersonId
                )
            )
        )
    }

    fun updateMerchant(
        merchantId: String,
        merchantItem: MerchantItem?,
        bankAccountId: String?,
        merchantDisplayName: String?,
        merchantDescription: String?,
        merchantLocation: Location?
    ) {
        // TODO: use ddb instead to handle null values
        val currentMerchantItem = merchantItem ?: MerchantItem(pk = MerchantItem.generatePk(merchantId), sk = MerchantItem.generateSk())
        val merchantData = currentMerchantItem.data
        val updatedMerchantItem = currentMerchantItem.copy(
            data = currentMerchantItem.data.copy(
                displayName = merchantDisplayName ?: merchantData.displayName,
                description = merchantDescription ?: merchantData.description,
                location = merchantLocation?.toDdbLocation() ?: merchantData.location,
                bankAccountId = bankAccountId ?: merchantData.bankAccountId,
            )
        )
        logger.info { "Updating merchant data with new values $updatedMerchantItem" }
        merchantTable.updateItem(
            UpdateItemEnhancedRequest.builder(MerchantItem::class.java)
                .item(updatedMerchantItem)
                .build()
        )
    }

    fun getMerchant(merchantId: String): MerchantItem? {
        val pk = MerchantItem.generatePk(merchantId)
        val sk = MerchantItem.generateSk()
        return try {
            merchantTable.getItem(
                Key.builder().partitionValue(pk).sortValue(sk).build()
            )
        } catch (e: ResourceNotFoundException) {
            null
        }
    }
}
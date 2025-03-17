package com.zenobiapay.table.user.dao

import com.zenobiapay.table.di.USER_TABLE_NAME
import com.zenobiapay.api.generated.models.Location as ApiLocation
import com.zenobiapay.table.user.model.Location
import com.zenobiapay.table.user.model.MerchantItem
import com.zenobiapay.table.user.model.UserItem
import com.zenobiapay.table.user.model.UserItemData
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
        merchantLocation: ApiLocation?,
        webhookUrl: String?
    ) {
        // TODO: use ddb instead to handle null values
        val currentMerchantItem = merchantItem ?: MerchantItem(pk = MerchantItem.generatePk(merchantId), sk = MerchantItem.generateSk())
        val merchantData = currentMerchantItem.data
        val location = if (merchantLocation != null) {
            Location.fromApiLocation(merchantLocation)
        } else {
            null
        }
        val updatedMerchantItem = currentMerchantItem.copy(
            data = currentMerchantItem.data.copy(
                displayName = merchantDisplayName ?: merchantData.displayName,
                description = merchantDescription ?: merchantData.description,
                location = location ?: merchantData.location,
                bankAccountId = bankAccountId ?: merchantData.bankAccountId,
                webhookUrl = webhookUrl ?: merchantData.webhookUrl
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
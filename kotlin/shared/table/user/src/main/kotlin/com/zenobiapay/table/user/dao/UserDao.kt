package com.zenobiapay.table.user.dao

import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.table.di.USER_TABLE_NAME
import com.zenobiapay.api.generated.models.Location as ApiLocation
import com.zenobiapay.table.user.model.Location
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.table.user.model.UserItem
import com.zenobiapay.table.user.model.UserItemData
import com.zenobiapay.table.user.model.UserType
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

    fun getUserItem(sub: String): UserItem? {
        val pk = UserItem.generatePk(sub)
        val sk = UserItem.generateSk()
        return try {
            userTable.getItem(
                Key.builder().partitionValue(pk).sortValue(sk).build()
            )
        } catch (e: ResourceNotFoundException) {
            null
        }
    }

    fun putUser(
        sub: String,
        firstName: String,
        lastName: String,
        orumPersonId: String,
        userType: UserType,
        isApproved: Boolean,
        merchantData: MerchantData
    ) {
        userTable.putItem(
            UserItem(
                pk = UserItem.generatePk(sub),
                sk = UserItem.generateSk(),
                data = UserItemData(
                    orumPersonId = orumPersonId,
                    isApproved = isApproved,
                    merchantData = merchantData,
                ),
                userType = userType,
            )
        )
    }

    fun updateMerchant(
        merchantId: String,
        bankAccountId: String?,
        merchantDisplayName: String?,
        merchantDescription: String?,
        merchantLocation: ApiLocation?,
        webhookUrl: String?
    ) {
        // TODO: use ddb instead to handle null values
        val currentMerchantItem = getUserItem(merchantId) ?: throw InvalidRequestException("")
        val merchantData = currentMerchantItem.data.merchantData
        val location = if (merchantLocation != null) {
            Location.fromApiLocation(merchantLocation)
        } else {
            null
        }
        val updatedMerchantItem = currentMerchantItem.copy(
            data = currentMerchantItem.data.copy(
                merchantData = MerchantData(
                    displayName = merchantDisplayName ?: merchantData?.displayName,
                    description = merchantDescription ?: merchantData?.description,
                    location = location ?: merchantData?.location,
                    bankAccountId = bankAccountId ?: merchantData?.bankAccountId,
                    webhookUrl = webhookUrl ?: merchantData?.webhookUrl
                ),
            )
        )
        logger.info { "Updating merchant data with new values $updatedMerchantItem" }
        userTable.updateItem(
            UpdateItemEnhancedRequest.builder(UserItem::class.java)
                .item(updatedMerchantItem)
                .build()
        )
    }
}
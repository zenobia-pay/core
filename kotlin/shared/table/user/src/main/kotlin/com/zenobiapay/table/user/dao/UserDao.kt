package com.zenobiapay.table.user.dao

import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.table.MAX_LIST_ITEMS
import com.zenobiapay.table.di.USER_TABLE_NAME
import com.zenobiapay.api.generated.models.Location as ApiLocation
import com.zenobiapay.table.user.model.Location
import com.zenobiapay.table.user.model.M2MCredentialsData
import com.zenobiapay.table.user.model.M2MCredentialsItem
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.table.user.model.UserItem
import com.zenobiapay.table.user.model.UserItemData
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class UserDao @Inject constructor(
    private val client: DynamoDbEnhancedClient,
    @Named(USER_TABLE_NAME) private val userTableName: String
) {
    private val userTable = client.table(userTableName, TableSchema.fromBean(UserItem::class.java))
    private val m2mCredentialsTable = client.table(userTableName, TableSchema.fromBean(M2MCredentialsItem::class.java))

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
                    firstName = firstName,
                    lastName = lastName,
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

    fun queryUsers(userType: UserType): PageIterable<UserItem>? {
        val request = ScanEnhancedRequest.builder()
            .filterExpression(
                Expression.builder()
                    .expression("userType = :userTypeVal")
                    .expressionValues(mapOf(":userTypeVal" to AttributeValue.builder().s(UserType.MERCHANT.name).build()))
                    .build()
            )
            .build()
        return userTable.scan(request)
    }

    fun putM2MCredentials(
        userId: String,
        m2mClientId: String,
        auth0ClientName: String
    ) {
        logger.info { "Putting m2m credentials" }
        m2mCredentialsTable.putItem(
            M2MCredentialsItem(
                pk = M2MCredentialsItem.generatePk(userId),
                sk = M2MCredentialsItem.generateSk(m2mClientId),
                data = M2MCredentialsData(
                    auth0ClientName = auth0ClientName,
                    clientId = m2mClientId
                )
            )
        )
    }

    fun getM2MCredentials(
        userId: String,
        clientId: String,
    ): M2MCredentialsItem? {
        logger.info { "Getting m2m credentials" }
        val pk = M2MCredentialsItem.generatePk(userId)
        val sk = M2MCredentialsItem.generateSk(clientId)

        val key = Key.builder().partitionValue(pk).sortValue(sk).build()
        return m2mCredentialsTable.getItem(key)
    }

    fun listM2MCredentials(
        userId: String,
    ): List<M2MCredentialsItem> {
        logger.info { "Listing m2m credentials" }
        val queryConditional = QueryConditional.keyEqualTo {
            it.partitionValue(M2MCredentialsItem.generatePk(userId))
        }
        val queryRequest = QueryEnhancedRequest.builder()
            .queryConditional(queryConditional)
            .scanIndexForward(false)
            .limit(MAX_LIST_ITEMS)
            .build()

        // TODO: handle pagination
        val toReturn = mutableListOf<M2MCredentialsItem>()
        m2mCredentialsTable.query(queryRequest)
            .stream().forEach {
                logger.info { "Got list response page ${it.items()}" }
                toReturn += it.items()
            }

        logger.info { "Returning accumulated list $toReturn" }
        return toReturn
    }

    fun deleteM2MCredentials(
        userId: String,
        clientId: String,
    ) {
        logger.info { "Removing m2m credentials" }
        val pk = M2MCredentialsItem.generatePk(userId)
        val sk = M2MCredentialsItem.generateSk(clientId)
        m2mCredentialsTable.deleteItem(
            Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build()
        )
    }
}
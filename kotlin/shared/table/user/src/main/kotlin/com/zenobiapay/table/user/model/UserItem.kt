package com.zenobiapay.table.user.model

import com.zenobiapay.api.generated.model.Location as ApiLocation
import com.zenobiapay.api.generated.model.UserType as ApiUserType
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey

@DynamoDbBean
data class UserItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    @get:DynamoDbSortKey
    var sk: String = "",
    @get:DynamoDbAttribute("data")
    var data: UserItemData = UserItemData(),
    var userType: UserType? = null,
    var ttl: Long? = null, // DO NOT change this value without changing user dao conditional expressions
) {
    companion object {
        fun generatePk(sub: String) = "USER#id_$sub"
        fun generateSk() = "DETAILS"
    }

    fun getSub(): String {
        val regex = """USER#id_(.*)""".toRegex()
        return regex.find(this.pk)!!.groupValues[1]
    }
}

@DynamoDbBean
data class UserItemData(
    var orumId: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var isApproved: Boolean = false,
    var merchantData: MerchantData? = null,
)

enum class UserType {
    MERCHANT,
    CUSTOMER;

    fun toApiUserType(): ApiUserType {
        return when (this) {
            MERCHANT -> ApiUserType.MERCHANT
            CUSTOMER -> ApiUserType.CUSTOMER
        }
    }

    companion object {
        fun toDdbUserType(userType: ApiUserType): UserType {
            return when (userType) {
                ApiUserType.MERCHANT -> MERCHANT
                ApiUserType.CUSTOMER -> CUSTOMER
            }
        }
    }
}

@DynamoDbBean
data class MerchantData(
    var displayName: String? = null,
    var description: String? = null,
    var location: Location? = null,
    var bankAccountId: String? = null,
    var webhookUrl: String? = null
)

@DynamoDbBean
data class Location(
    var address: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null
) {
    companion object {
        fun fromApiLocation(location: ApiLocation): Location {
            return Location(
                address = location.address,
                latitude = location.latitude?.toDouble(),
                longitude = location.longitude?.toDouble()
            )
        }
    }
}

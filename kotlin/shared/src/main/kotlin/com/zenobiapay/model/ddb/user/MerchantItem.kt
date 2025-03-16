package com.zenobiapay.model.ddb.user

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey
import com.zenobiapay.api.generated.models.Location as ApiLocation

@DynamoDbBean
data class MerchantItem(
    @get:DynamoDbPartitionKey
    var pk: String = "",
    @get:DynamoDbSortKey
    var sk: String = "",
    @get:DynamoDbAttribute("data")
    var data: MerchantItemData = MerchantItemData()
) {
    companion object {
        fun generatePk(sub: String) = "MERCHANT#m_$sub"
        fun generateSk() = "DETAILS"
    }
}

@DynamoDbBean
data class MerchantItemData(
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

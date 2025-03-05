package com.zenobiapay.model.api.account

data class UpdateMerchantRequest(
    val bankAccountId: String? = null,
    val merchantDisplayName: String? = null,
    val merchantDescription: String? = null,
    val merchantLocation: Location? = null,
    val webhookUrl: String? = null,
)

data class Location(
    val address: String,
    val latitude: Double,
    val longitude: Double,
) {
    fun toDdbLocation(): com.zenobiapay.model.ddb.user.Location {
        return com.zenobiapay.model.ddb.user.Location(
            address,
            latitude,
            longitude,
        )
    }
}

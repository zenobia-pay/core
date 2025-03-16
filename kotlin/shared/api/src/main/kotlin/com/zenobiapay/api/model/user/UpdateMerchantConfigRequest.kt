package com.zenobiapay.api.model.user

import com.zenobiapay.api.generated.models.Location

data class UpdateMerchantConfigRequest(
    val bankAccountId: String?,
    val merchantDisplayName: String?,
    val merchantDescription: String?,
    val webhookUrl: String?,
    val merchantLocation: Location?
)

package com.zenobiapay.model.api.user

import com.zenobiapay.generated.models.Location

data class UpdateMerchantConfigRequest(
    val bankAccountId: String?,
    val merchantDisplayName: String?,
    val merchantDescription: String?,
    val webhookUrl: String?,
    val merchantLocation: Location?,
)

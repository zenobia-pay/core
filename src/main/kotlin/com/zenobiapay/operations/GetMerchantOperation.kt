package com.zenobiapay.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.dao.UserDao
import com.zenobiapay.generated.models.GetMerchant200Response
import com.zenobiapay.generated.models.GetMerchant200ResponseMerchantLocation
import com.zenobiapay.model.cognito.UserPoolGroup
import javax.inject.Inject

class GetMerchantOperation @Inject constructor(private val userDao: UserDao): Operation() {
    override fun run(input: APIGatewayProxyRequestEvent, context: Context, userId: String): Any {
        val merchantItem = userDao.getMerchant(userId)

        if (merchantItem == null) {
            return GetMerchant200Response(
                bankAccountId = null,
                merchantDisplayName = null,
                merchantDescription = null,
                webhookUrl = null,
                merchantLocation = null
            )
        }

        return GetMerchant200Response(
            bankAccountId = merchantItem.data.bankAccountId,
            merchantDisplayName = merchantItem.data.displayName,
            merchantDescription = merchantItem.data.description,
            webhookUrl = merchantItem.data.webhookUrl,
            merchantLocation = GetMerchant200ResponseMerchantLocation(
                address = merchantItem.data.location?.address,
                latitude = merchantItem.data.location?.latitude?.toBigDecimal(),
                longitude = merchantItem.data.location?.longitude?.toBigDecimal(),
            )
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}
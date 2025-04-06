package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetMerchantConfig200Response
import com.zenobiapay.api.generated.model.Location
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import javax.inject.Inject

class GetMerchantConfigOperation @Inject constructor(
    private val userDao: UserDao
): Operation<NoApiBody, GetMerchantConfig200Response>() {

    override val inputType = NoApiBody::class.java

    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): GetMerchantConfig200Response {
        val merchantItem = userDao.getUserItem(userId!!)
        if (merchantItem == null) {
            return GetMerchantConfig200Response()
                .bankAccountId(null)
                .merchantDisplayName(null)
                .merchantDescription(null)
                .webhookUrl(null)
                .merchantLocation(null)
        }
        val merchantData = merchantItem.data.merchantData

        return GetMerchantConfig200Response()
            .bankAccountId(merchantData?.bankAccountId)
            .merchantDisplayName(merchantData?.displayName)
            .merchantDescription(merchantData?.description)
            .webhookUrl(merchantData?.webhookUrl)
            .merchantLocation(Location()
                .address(merchantData?.location?.address)
                .latitude(merchantData?.location?.latitude?.toBigDecimal())
                .longitude(merchantData?.location?.longitude?.toBigDecimal())
            )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT)
    }
}

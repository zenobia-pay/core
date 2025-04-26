package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.model.GetUserProfile200Response
import com.zenobiapay.api.model.NoApiBody
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import jakarta.inject.Inject

class GetUserProfileOperation @Inject constructor(
    private val userDao: UserDao,
): Operation<NoApiBody, GetUserProfile200Response>() {

    override val inputType = NoApiBody::class.java

    override fun run(request: NoApiBody, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): GetUserProfile200Response {
        userId!!
        val user = userDao.getUserItem(userId)
        val hasOnboarded = user != null
        return GetUserProfile200Response()
            .hasOnboarded(hasOnboarded)
            .userType(user?.userType?.toApiUserType())
            .isApproved(user?.data?.isApproved == true)
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN, UserPoolGroup.MERCHANT, UserPoolGroup.CUSTOMER)
    }
}
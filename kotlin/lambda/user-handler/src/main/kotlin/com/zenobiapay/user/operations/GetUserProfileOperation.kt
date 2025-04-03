package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.zenobiapay.api.generated.models.GetUserProfile200Response
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import javax.inject.Inject

class GetUserProfileOperation @Inject constructor(
    private val userDao: UserDao,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val user = userDao.getUserItem(userId)
        val hasOnboarded = user != null
        return GetUserProfile200Response(
            hasOnboarded = hasOnboarded,
            userType = user?.userType?.toApiUserType(),
            isApproved = user?.data?.isApproved == true
        )
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}
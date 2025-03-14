package com.zenobiapay.util

import com.zenobiapay.di.USER_POOL_ID
import com.zenobiapay.model.cognito.UserPoolGroup
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType
import javax.inject.Inject
import javax.inject.Named

private val logger = KotlinLogging.logger {}

class CognitoUtil @Inject constructor(
    private val cognitoClient: CognitoIdentityProviderClient,
    @Named(USER_POOL_ID) private val userPoolId: String
) {

    companion object {
        private const val MERCHANT_GROUP_NAME = "MerchantGroup"
        private const val CUSTOMER_GROUP_NAME = "CustomerGroup"
    }

    fun getUserFullName(userName: String): String {
        val response = cognitoClient.adminGetUser {
            it.userPoolId(userPoolId)
                .username(userName)
        }
        val userAttributes = response.userAttributes()
        val givenName = searchUserAttributes(userAttributes, "given_name")
        val familyName = searchUserAttributes(userAttributes, "family_name")

        logger.info { "Got response $response, ${response.userAttributes()}" }
        return "$givenName $familyName"
    }

    fun addUserToGroup(userName: String, userPoolGroup: UserPoolGroup) {
        cognitoClient.adminAddUserToGroup {
            it.groupName(userPoolGroup.value)
                .userPoolId(userPoolId)
                .username(userName)
        }
    }

    fun listMerchantUserIds(): MutableListIterator<UserType> {
        val users = cognitoClient.listUsersInGroup {
            it.userPoolId(userPoolId)
                .groupName(MERCHANT_GROUP_NAME)
        }.users()
        logger.info { "Got ${users.size} merchants" }
        return users.listIterator()
    }

    fun createUserPoolClient(clientName: String): UserPoolClientType {
        val request = CreateUserPoolClientRequest.builder()
            .userPoolId(userPoolId)
            .clientName(clientName)
            .generateSecret(true)
            .allowedOAuthFlows(OAuthFlowType.CLIENT_CREDENTIALS)
            .allowedOAuthFlowsUserPoolClient(true)
            .allowedOAuthScopes("Zenobia/machine")
            .build()
        return cognitoClient.createUserPoolClient(request).userPoolClient()
    }

    private fun searchUserAttributes(userAttributes: List<AttributeType>, fieldName: String): String {
        return userAttributes.first { it.name() == fieldName }.value()
    }
}

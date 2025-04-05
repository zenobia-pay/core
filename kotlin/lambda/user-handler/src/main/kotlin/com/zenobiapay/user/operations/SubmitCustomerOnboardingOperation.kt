package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.plaid.client.model.IdentityVerificationGetResponse
import com.plaid.client.model.IdentityVerificationStatus
import com.zenobiapay.api.model.exception.IdentityFailedException
import com.zenobiapay.api.model.exception.IdentityNotSuccessfulException
import com.zenobiapay.api.generated.model.SubmitCustomerOnboardingRequest
import com.zenobiapay.api.generated.model.UserType
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.util.getEmail
import com.zenobiapay.orum.OrumException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.orum.model.Person
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType as DdbUserType
import com.zenobiapay.user.util.Auth0Wrapper
import com.zenobiapay.user.util.Auth0Wrapper.Companion.ROLE_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class SubmitCustomerOnboardingOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val plaidWrapper: PlaidWrapper,
    private val orumWrapper: OrumWrapper,
    private val auth0Wrapper: Auth0Wrapper,
    private val userDao: UserDao,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val request = objectMapper.readValue(input.body, SubmitCustomerOnboardingRequest::class.java)
        val identityVerificationResponse = plaidWrapper.getIdentityVerification(request.identityVerificationId)
        checkStatus(identityVerificationResponse.status)

        val person = createOrumPerson(userId, identityVerificationResponse, input.requestContext.getEmail()!!)
        auth0Wrapper.putAppMetadataOnUser(userId, mapOf(ROLE_KEY to UserType.CUSTOMER.name))
        userDao.putUser(
            userId,
            person.firstName,
            person.lastName,
            person.id,
            DdbUserType.toDdbUserType(UserType.CUSTOMER),
            isApproved = true,
        )

        return EmptyApiResponse()
    }

    private fun checkStatus(status: IdentityVerificationStatus) {
        logger.info { "Checking identity verification status $status completed successfully" }
        when (status) {
            IdentityVerificationStatus.SUCCESS -> Unit
            IdentityVerificationStatus.FAILED -> throw IdentityFailedException()
            IdentityVerificationStatus.ACTIVE, IdentityVerificationStatus.EXPIRED, IdentityVerificationStatus.CANCELED, IdentityVerificationStatus.PENDING_REVIEW
                -> throw IdentityNotSuccessfulException()
            IdentityVerificationStatus.ENUM_UNKNOWN -> throw Exception("Failed to parse identity verification status")
        }
    }

    private fun createOrumPerson(userId: String, identityResponse: IdentityVerificationGetResponse, email: String): Person {
        val name = identityResponse.user.name ?: throw Exception("Could not get user name from identity response")
        val createPersonRequest = OrumCreatePersonRequest(
            customerReferenceId = generateCustomerOrumId(userId),
            firstName = name.givenName,
            lastName = name.familyName,
            socialSecurityNumber = null,
            contacts = listOf(Contact(type = "email", value = email))
        )

        val person = try {
            orumWrapper.createPerson(createPersonRequest).person.also {
                logger.info { "Created Orum person with customer reference id $userId" }
            }
        } catch (e: OrumException) {
            if (e.isCreatePersonAlreadyExistsException()) {
                logger.info { "Person $userId already exists. Updating pre-existing person with new info" }
                orumWrapper.updatePerson(createPersonRequest).person
            } else {
                throw e
            }
        }
        logger.info { "Got person $person" }
        return person
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER)
    }
}
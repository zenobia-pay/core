package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.api.generated.models.SubmitOnboardingRequest
import com.zenobiapay.api.generated.models.UserType
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.model.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.orum.OrumException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.table.user.model.UserType as DdbUserType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class SubmitOnboardingOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val userDao: UserDao,
    private val orumWrapper: OrumWrapper,
): Operation() {
    override fun run(
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): Any {
        userId!!
        val request = objectMapper.readValue(input.body, SubmitOnboardingRequest::class.java)

        if (userDao.getUserItem(userId) != null) {
            throw InvalidRequestException("User has already onboarded")
        }

        val createPersonRequest = OrumCreatePersonRequest(
            customerReferenceId = userId,
            firstName = request.firstName,
            lastName = request.lastName,
            socialSecurityNumber = null,
            contacts = listOf(Contact(type = "email", value = "${UUID.randomUUID()}@gmail.com")) // TODO: fetch email
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
        val isAutoApproved = request.userType == UserType.CUSTOMER
        userDao.putUser(
            userId,
            request.firstName,
            request.lastName,
            person.id,
            DdbUserType.toDdbUserType(request.userType),
            isAutoApproved,
            MerchantData(
                displayName = request.merchantDisplayName,
            )
        )
        logger.info { "Successfully put person into table" }
        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.MERCHANT, UserPoolGroup.CUSTOMER)
    }
}
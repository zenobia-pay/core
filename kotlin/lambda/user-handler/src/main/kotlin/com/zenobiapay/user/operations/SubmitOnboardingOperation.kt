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
import com.zenobiapay.api.util.getEmail
import com.zenobiapay.orum.OrumException
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.Address
import com.zenobiapay.orum.model.BusinessEntityType
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.OrumCreateBusinessRequest
import com.zenobiapay.orum.model.OrumCreateBusinessResponse
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.orum.model.Person
import com.zenobiapay.orum.model.TaxIdType
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.user.util.Auth0Wrapper
import com.zenobiapay.user.util.Auth0Wrapper.Companion.ROLE_KEY
import com.zenobiapay.table.user.model.UserType as DdbUserType
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class SubmitOnboardingOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val userDao: UserDao,
    private val orumWrapper: OrumWrapper,
    private val auth0Wrapper: Auth0Wrapper,
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
        val email = input.requestContext.getEmail() ?: throw Exception("email not found")
        val orumId = when (request.userType) {
            UserType.CUSTOMER -> createPerson(userId, request, email).id
            UserType.MERCHANT -> {
                assertNotNull(
                    request.merchantDisplayName,
                    request.legalBusinessName,
                    request.entityType,
                    request.taxId,
                    request.taxIdType,
                    request.incorporationDate,
                    request.address,
                )
                createMerchant(userId, request, email).business.id
            }
        }

        logger.info { "Adding role ${request.userType} to user $userId"}
        val role = UserPoolGroup.fromString(request.userType.value)
        if (role == UserPoolGroup.UNKNOWN) {
            logger.error { "Could not get role from request's usertype ${request.userType.value}"}
            throw InvalidRequestException("Unknown role for submit onboarding")
        }
        auth0Wrapper.putAppMetadataOnUser(userId, mapOf(ROLE_KEY to request.userType.value))

        val isAutoApproved = request.userType == UserType.CUSTOMER
        userDao.putUser(
            userId,
            request.firstName,
            request.lastName,
            orumId,
            DdbUserType.toDdbUserType(request.userType),
            isAutoApproved,
            MerchantData(
                displayName = request.merchantDisplayName,
            )
        )
        logger.info { "Successfully put person into table" }
        return EmptyApiResponse()
    }

    private fun createPerson(userId: String, request: SubmitOnboardingRequest, email: String): Person {
        val createPersonRequest = OrumCreatePersonRequest(
            customerReferenceId = generateCustomerOrumId(userId),
            firstName = request.firstName,
            lastName = request.lastName,
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

    private fun createMerchant(userId: String, request: SubmitOnboardingRequest, email: String): OrumCreateBusinessResponse {
        val name = "${request.firstName} ${request.lastName}"
        val createBusinessRequest = OrumCreateBusinessRequest(
            customerReferenceId = generateMerchantOrumId(userId),
            legalName = name,
            businessName = request.legalBusinessName,
            entityType = request.entityType!!.let { BusinessEntityType.valueOf(it.value) },
            taxId = request.taxId,
            taxIdType = TaxIdType.valueOf(request.taxIdType!!.value),
            accountHolderName = name,
            incorporationDate = request.incorporationDate,
            addresses = listOf(request.address!!.let {
                Address(
                    address1 = it.address1,
                    address2 = it.address2,
                    city = it.city,
                    state = it.state,
                    country = it.country.value,
                    zip5 = it.zip5,
                )
            }),
            contacts = listOf(Contact("email", email))
        )
        return orumWrapper.createBusiness(createBusinessRequest).also {
            logger.info { "Created Orum business with business reference id $userId and orum reference ${it.business.id}" }
        }
    }

    private fun assertNotNull(vararg values: Any?) {
        values.forEach {
            if (it == null) {
                throw InvalidRequestException("Merchant fields not passed")
            }
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}
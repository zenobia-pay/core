package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.exception.InvalidRequestException
import com.zenobiapay.api.generated.models.SubmitMerchantOnboardingRequest
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
import com.zenobiapay.orum.model.TaxIdType
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.MerchantData
import com.zenobiapay.table.user.model.UserType as DdbUserType
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class SubmitMerchantOnboardingOperation @Inject constructor(
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
        val request = objectMapper.readValue(input.body, SubmitMerchantOnboardingRequest::class.java)

        if (userDao.getUserItem(userId) != null) {
            throw InvalidRequestException("User has already onboarded")
        }
        val email = input.requestContext.getEmail() ?: throw Exception("email not found")
        val merchantId = createMerchant(userId, request, email).business.id

        userDao.putUser(
            userId,
            request.firstName,
            request.lastName,
            merchantId,
            DdbUserType.toDdbUserType(UserType.MERCHANT),
            false,
            MerchantData(
                displayName = request.merchantDisplayName,
            )
        )
        logger.info { "Successfully put person into table" }
        return EmptyApiResponse()
    }

    private fun createMerchant(userId: String, request: SubmitMerchantOnboardingRequest, email: String): OrumCreateBusinessResponse {
        val name = "${request.firstName} ${request.lastName}"
        val createBusinessRequest = OrumCreateBusinessRequest(
            customerReferenceId = generateMerchantOrumId(userId),
            legalName = name,
            businessName = request.legalBusinessName,
            entityType = BusinessEntityType.valueOf(request.entityType.value),
            taxId = request.taxId,
            taxIdType = TaxIdType.valueOf(request.taxIdType.value),
            accountHolderName = name,
            incorporationDate = request.incorporationDate,
            addresses = listOf(request.address.let {
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

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN)
    }
}
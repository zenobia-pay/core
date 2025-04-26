package com.zenobiapay.user.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.model.SubmitTermsRequest
import com.zenobiapay.api.model.EmptyApiResponse
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.AgreementMetadata
import com.zenobiapay.user.di.UserModule
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject
import jakarta.inject.Named
import java.time.Instant

private val logger = KotlinLogging.logger {}

class SubmitTermsOperation @Inject constructor(
    private val objectMapper: ObjectMapper,
    private val userDao: UserDao,
    @Named(UserModule.PRIVACY_TERMS_VERSION) private val privacyTermsVersion: String,
    @Named(UserModule.DEBIT_AUTH_VERSION) private val debitAuthVersion: String,

): Operation<SubmitTermsRequest, EmptyApiResponse>() {

    override val inputType = SubmitTermsRequest::class.java

    override fun run(
        request: SubmitTermsRequest,
        input: APIGatewayProxyRequestEvent,
        context: Context,
        userId: String?
    ): EmptyApiResponse {
        userId!!
        val termsAuthMetadata = if (request.termsAuthorization == true) {
            AgreementMetadata(
                agreed = true,
                version = privacyTermsVersion,
                ip = input.requestContext.identity.sourceIp,
                agreedTime = Instant.now().toString(),
                userAgent = input.requestContext.identity.userAgent,
                requestId = input.requestContext.requestId
            )
        } else null
        val debitAuthMetadata = if (request.debitAuthorization) {
            AgreementMetadata(
                agreed = true,
                version = debitAuthVersion,
                ip = input.requestContext.identity.sourceIp,
                agreedTime = Instant.now().toString(),
                userAgent = input.requestContext.identity.userAgent,
                requestId = input.requestContext.requestId
            )
        } else null
        userDao.updateTerms(
            userId,
            termsAuthMetadata,
            debitAuthMetadata,
        )

        return EmptyApiResponse()
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}
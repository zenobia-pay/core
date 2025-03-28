package com.zenobiapay.user.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.exception.UnknownPathException
import com.zenobiapay.api.generated.models.SubmitOnboardingRequest
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.user.di.DaggerAppComponent
import com.zenobiapay.user.operations.GetMerchantConfigOperation
import com.zenobiapay.user.operations.GetUserProfileOperation
import com.zenobiapay.user.operations.SubmitOnboardingOperation
import com.zenobiapay.user.operations.UpdateMerchantConfigOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class UserHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    @Inject
    lateinit var responseHandler: ResponseHandler

    @Inject
    lateinit var updateMerchantConfigOperation: UpdateMerchantConfigOperation

    @Inject
    lateinit var getMerchantConfigOperation: GetMerchantConfigOperation

    @Inject
    lateinit var getUserProfileOperation: GetUserProfileOperation

    @Inject
    lateinit var submitOnboardingOperation: SubmitOnboardingOperation

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        logger.info { "Got input $input" }
        val operation = when (input?.path) {
            "/update-merchant-config" -> updateMerchantConfigOperation
            "/get-merchant-config" -> getMerchantConfigOperation
            "/get-user-profile" -> getUserProfileOperation
            "/submit-onboarding" -> submitOnboardingOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}

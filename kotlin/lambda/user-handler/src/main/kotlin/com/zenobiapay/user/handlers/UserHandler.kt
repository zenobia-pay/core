package com.zenobiapay.user.handlers

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.util.ResponseHandler
import com.zenobiapay.user.di.DaggerAppComponent
import com.zenobiapay.user.operations.CreateM2MCredentialsOperation
import com.zenobiapay.user.operations.DeleteM2MCredentialsOperation
import com.zenobiapay.user.operations.GetMerchantConfigOperation
import com.zenobiapay.user.operations.GetUserProfileOperation
import com.zenobiapay.user.operations.ListM2MCredentialsOperation
import com.zenobiapay.user.operations.ListMerchantsOperation
import com.zenobiapay.user.operations.SubmitTermsOperation
import com.zenobiapay.user.operations.UpdateMerchantConfigOperation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.inject.Inject

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
    lateinit var submitTermsOperation: SubmitTermsOperation

    @Inject
    lateinit var createM2MCredentialsOperation: CreateM2MCredentialsOperation

    @Inject
    lateinit var listM2MCredentialsOperation: ListM2MCredentialsOperation

    @Inject
    lateinit var deleteM2MCredentialsOperation: DeleteM2MCredentialsOperation

    @Inject
    lateinit var listMerchantsOperation: ListMerchantsOperation

    @Inject
    lateinit var objectMapper: ObjectMapper

    init {
        DaggerAppComponent.create().inject(this)
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent?, context: Context?): APIGatewayProxyResponseEvent {
        logger.info { "Got input ${input?.body}" }
        val operation = when (input?.path) {
            "/update-merchant-config" -> updateMerchantConfigOperation
            "/get-merchant-config" -> getMerchantConfigOperation
            "/get-user-profile" -> getUserProfileOperation
            "/submit-terms" -> submitTermsOperation
            "/create-m2m-credentials" -> createM2MCredentialsOperation
            "/list-m2m-credentials" -> listM2MCredentialsOperation
            "/delete-m2m-credentials" -> deleteM2MCredentialsOperation
            "/list-merchants" -> listMerchantsOperation
            else -> return responseHandler.generateApiGatewayErrorResponse(UnknownPathException())
        }
        return responseHandler.returnApiGwResponse(operation, input, context!!)
    }
}

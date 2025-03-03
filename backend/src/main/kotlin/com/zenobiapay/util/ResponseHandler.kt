package com.zenobiapay.util

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.model.ApiError
import com.zenobiapay.model.exception.ResourceNotFoundException
import com.zenobiapay.model.exception.UnauthorizedException
import com.zenobiapay.model.exception.ZenobiaExternalException
import com.zenobiapay.operations.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class ResponseHandler @Inject constructor(private val objectMapper: ObjectMapper) {
    fun returnApiGwResponse(operation: Operation, input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        try {
            operation.assertUserPoolGroupValid(input.requestContext.getUserPoolGroups())
            val userId = input.requestContext.getUserId(objectMapper)
            val response = operation.run(input, context, userId)
            return APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(response))
        } catch (e: ResourceNotFoundException) {
            logger.error(e) {
                "Caught resource not found exception, relaying back to customer"
            }
            return APIGatewayProxyResponseEvent()
                .withStatusCode(404)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(ApiError(e.message)))
        } catch (e: UnauthorizedException) {
            logger.error(e) {
                "Caught unauthorized exception, returning 403 to customer"
            }
            return APIGatewayProxyResponseEvent()
                .withStatusCode(403)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(ApiError(e.message)))
        } catch (e: ZenobiaExternalException) {
            logger.error(e) {
                "Caught external exception, returning 4xx to customer"
            }
            return APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(ApiError(e.message)))
        } catch (e: Exception) {
            logger.error(e) { "Got unhandled exception. Throwing internal error to customer" }
            return APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(getCorsHeaders())
                .withBody(objectMapper.writeValueAsString(ApiError("An internal error occurred.")))
        }
    }
}

package com.zenobiapay.util

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.generated.models.ErrorResponse
import com.zenobiapay.model.exception.ResourceNotFoundException
import com.zenobiapay.model.exception.UnauthorizedException
import com.zenobiapay.model.exception.UnknownPathException
import com.zenobiapay.model.exception.ZenobiaExternalException
import com.zenobiapay.operation.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class ResponseHandler @Inject constructor(private val objectMapper: ObjectMapper) {
    fun returnApiGwResponse(operation: Operation, input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        return wrapOperation {
            val userId = input.requestContext.getUserId(objectMapper)
            operation.run(input, context, userId)
        }
    }

    fun wrapOperation(body: () -> Any): APIGatewayProxyResponseEvent {
        return try {
            generateSuccessResponse(body())
        } catch (e: Exception) {
            generateApiGatewayErrorResponse(e)
        }
    }

    fun generateSuccessResponse(response: Any): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(getCorsHeaders())
            .withBody(objectMapper.writeValueAsString(response))
    }

    fun generateApiGatewayErrorResponse(error: Exception): APIGatewayProxyResponseEvent {
        val (errorCode, status) = when (error) {
            is ResourceNotFoundException, is UnknownPathException -> 404 to error.message
            is UnauthorizedException -> 403 to error.message
            is ZenobiaExternalException -> 400 to error.message
            else -> 500 to "An internal error has occurred"
        }
        logger.error(error) { "Caught exception, returning $errorCode, $status to customer" }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(errorCode)
            .withHeaders(getCorsHeaders())
            .withBody(
                objectMapper.writeValueAsString(
                    ErrorResponse(message = status, error = getErrorString(errorCode))
                )
            )
    }

    private fun getErrorString(statusCode: Int): String {
        return when (statusCode) {
            404 -> "NotFound"
            403 -> "AccessDenied"
            400 -> "BadRequest"
            500 -> "InternalServerError"
            else -> "InternalServerError"
        }
    }
}

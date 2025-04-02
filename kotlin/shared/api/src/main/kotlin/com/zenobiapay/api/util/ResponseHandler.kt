package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.api.exception.ResourceNotFoundException
import com.zenobiapay.api.exception.UnauthorizedException
import com.zenobiapay.api.exception.UnknownPathException
import com.zenobiapay.api.exception.ZenobiaExternalException
import com.zenobiapay.api.generated.models.ErrorResponse
import com.zenobiapay.api.model.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.logging.log4j.ThreadContext
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class ResponseHandler @Inject constructor(private val objectMapper: ObjectMapper) {
    fun returnApiGwResponse(operation: Operation, input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        setLoggingContext(input.requestContext.requestId, input.requestContext.getUserId())
        return wrapOperation {
            val userId = input.requestContext.getUserId()
            val role = input.requestContext.getUserRole()
            if (role !in operation.getUserPoolAllowList()) {
                logger.error { "Role $role not allowed for operation ${operation.javaClass.name} with allowed list ${operation.getUserPoolAllowList()}"}
                throw UnauthorizedException()
            }
            operation.run(input, context, userId)
        }
    }

    private fun wrapOperation(body: () -> Any): APIGatewayProxyResponseEvent {
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

    private fun setLoggingContext(requestId: String?, sub: String?) {
        requestId?.let { ThreadContext.put("requestId", it) }
        sub?.let { ThreadContext.put("sub", it) }
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
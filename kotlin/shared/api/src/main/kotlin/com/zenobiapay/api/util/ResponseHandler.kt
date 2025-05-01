package com.zenobiapay.api.util

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.zenobia.metric.MetricHelper
import com.zenobiapay.api.model.exception.ResourceNotFoundException
import com.zenobiapay.api.model.exception.ServiceQuotaExceededException
import com.zenobiapay.api.model.exception.UnauthorizedException
import com.zenobiapay.api.model.exception.UnknownPathException
import com.zenobiapay.api.model.exception.ZenobiaExternalException
import com.zenobiapay.api.generated.model.ErrorResponse
import com.zenobiapay.api.model.exception.DeclinedException
import com.zenobiapay.api.model.exception.InsufficientFundsException
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.model.exception.TransferStatusException
import com.zenobiapay.api.operation.Operation
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.apache.logging.log4j.ThreadContext
import jakarta.inject.Inject
import java.time.Instant
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class ResponseHandler @Inject constructor(
    val objectMapper: ObjectMapper,
    val metricHandler: MetricHelper
) {
    private val validator: Validator by lazy {
        Validation.buildDefaultValidatorFactory().validator
    }

    fun <I, O> returnApiGwResponse(operation: Operation<I, O>, input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        return wrapOperation(input.path) {
            val userId = input.requestContext.getUserId()
            setLoggingContext(input.requestContext.requestId, userId, input.path)
            logger.info { "Got operation ${operation.javaClass.simpleName}, userId $userId, body ${input.body}"}
            val role = input.requestContext.getUserRole()
            if (role !in operation.getUserPoolAllowList()) {
                logger.error { "Role $role not allowed for operation ${operation.javaClass.name} with allowed list ${operation.getUserPoolAllowList()}"}
                throw UnauthorizedException()
            }
            // Read empty map if no body is provided. Should be cast to NoApiBody class
            val request = try {
                objectMapper.readValue(input.body ?: "{}", operation.inputType)
            } catch (e: ValueInstantiationException) {
                logger.error(e) { "Failed to read body (enum?), throwing validation exception"}
                throw InvalidRequestException("Invalid input")
            } catch (e: UnrecognizedPropertyException) {
                logger.error(e) { "Unrecognized property provided"}
                throw InvalidRequestException("Unrecognized property provided")
            }

            val violations = validator.validate(request)
            if (violations.isNotEmpty()) {
                throw InvalidRequestException("${violations.first().propertyPath} ${violations.first().message}")
            }
            operation.run(request, input, context, userId)
        }
    }

    private fun <O> wrapOperation(path: String, body: () -> O): APIGatewayProxyResponseEvent {
        val startTime = System.currentTimeMillis()
        return try {
            metricHandler.emitSuccessMetric("Success", mapOf("path" to path)) {
                generateSuccessResponse(body())
            }
        } catch (e: Exception) {
            generateApiGatewayErrorResponse(e, path)
        } finally {
            val latency = System.currentTimeMillis() - startTime
            logger.info { "Got latency $latency" }
            metricHandler.putMetric("LatencyMillis", latency.toDouble(), dimensions = mapOf("path" to path))

            ThreadContext.clearAll()
        }
    }

    private fun <O> generateSuccessResponse(response: O): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withHeaders(getCorsHeaders())
            .withBody(objectMapper.writeValueAsString(response))
    }

    fun generateApiGatewayErrorResponse(error: Exception, path: String? = null): APIGatewayProxyResponseEvent {
        val (errorCode, status) = when (error) {
            is ResourceNotFoundException, is UnknownPathException -> 404 to error.message
            is UnauthorizedException -> 403 to error.message
            is ServiceQuotaExceededException -> 429 to error.message
            is ZenobiaExternalException -> 400 to error.message
            else -> 500 to "An internal error has occurred"
        }
        logger.error(error) { "Caught exception, returning $errorCode, $status to customer" }
        val metricsDimensions = path?.let { mapOf("path" to path) } ?: mapOf()
        if (errorCode < 500 && errorCode >= 400) {
            metricHandler.putMetric("Error", 1.0, metricsDimensions)
        } else if (errorCode >= 500) {
            metricHandler.putMetric("Fault", 1.0, metricsDimensions)
        }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(errorCode)
            .withHeaders(getCorsHeaders())
            .withBody(
                objectMapper.writeValueAsString(
                    ErrorResponse().error(getErrorString(error, errorCode)).message(status)
                )
            )
    }

    private fun setLoggingContext(requestId: String?, sub: String?, path: String?) {
        requestId?.let { ThreadContext.put("requestId", it) }
        sub?.let { ThreadContext.put("sub", it) }
        path?.let { ThreadContext.put("path", it) }
    }

    private fun getErrorString(error: Exception, statusCode: Int): String {
        if (error is InsufficientFundsException) return "InsufficientFunds"
        if (error is DeclinedException) return "Declined"
        if (error is TransferStatusException) return "InvalidTransferStatus"
        return when (statusCode) {
            404 -> "NotFound"
            403 -> "AccessDenied"
            400 -> "BadRequest"
            500 -> "InternalServerError"
            else -> "InternalServerError"
        }
    }
}
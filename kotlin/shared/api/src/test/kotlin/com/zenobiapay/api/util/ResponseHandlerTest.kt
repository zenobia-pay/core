package com.zenobiapay.api.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.api.generated.models.ErrorResponse
import com.zenobiapay.api.exception.ResourceNotFoundException
import com.zenobiapay.api.exception.UnauthorizedException
import com.zenobiapay.api.exception.UnknownPathException
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ResponseHandlerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `test generateApiGatewayErrorResponse on ResourceNotFoundException returns correct error`() {
        val responseHandler = ResponseHandler(objectMapper)
        val response = responseHandler.generateApiGatewayErrorResponse(ResourceNotFoundException("TEST"))

        val body = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertEquals(404, response.statusCode)
        assertEquals("NotFound", body.error)
        assertEquals("Could not find resource TEST", body.message)
    }

    @Test
    fun `test generateApiGatewayErrorResponse on UnknownPathException returns correct error`() {
        val responseHandler = ResponseHandler(objectMapper)
        val response = responseHandler.generateApiGatewayErrorResponse(UnknownPathException())

        val body = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertEquals(404, response.statusCode)
        assertEquals("NotFound", body.error)
        assertEquals("Unknown endpoint", body.message)
    }

    @Test
    fun `test generateApiGatewayErrorResponse on Unauthorized exception returns correct error`() {
        val responseHandler = ResponseHandler(objectMapper)
        val response = responseHandler.generateApiGatewayErrorResponse(UnauthorizedException())

        val body = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertEquals(403, response.statusCode)
        assertEquals("AccessDenied", body.error)
        assertEquals("Unauthorized endpoint", body.message)
    }

    @Test
    fun `test generateApiGatewayErrorResponse on unhandled exception returns correct error`() {
        val responseHandler = ResponseHandler(objectMapper)
        val response = responseHandler.generateApiGatewayErrorResponse(NullPointerException())

        val body = objectMapper.readValue(response.body, ErrorResponse::class.java)
        assertEquals(500, response.statusCode)
        assertEquals("InternalServerError", body.error)
        assertEquals("An internal error has occurred", body.message)
    }
}

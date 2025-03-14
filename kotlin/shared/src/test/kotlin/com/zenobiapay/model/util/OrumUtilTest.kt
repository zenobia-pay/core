package com.zenobiapay.model.util

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zenobiapay.model.orum.OrumCreatePersonRequest
import com.zenobiapay.model.orum.OrumCredentials
import com.zenobiapay.util.OrumException
import com.zenobiapay.util.OrumUtil
import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrumUtilTest {
    private val httpClient = mockk<OkHttpClient>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()
    private val orumCredentials = OrumCredentials("clientId", "clientSecret")
    private val orumUtil = OrumUtil(httpClient, objectMapper, orumCredentials)

    @Test
    fun `test updatePerson`() {
        val personId = "personId"
        val customerReferenceId = "customerReferenceId"
        val firstName = "firstName"
        val lastName = "lastName"
        val dob = "dob"
        every {
            httpClient.newCall(any())
        } returns mockResponse(getMockCredentialsResponse()) andThen mockResponse(
            """
            {
              "person": {
                "id": "$personId",
                "customer_reference_id": "$customerReferenceId",
                "first_name": "$firstName",
                "middle_name": "middle name",
                "last_name": "$lastName",
                "date_of_birth": "$dob",
                "status": "created",
                "addresses": [
                  {
                    "id": "3c90c3cc-0d44-4b50-8888-8dd25736052a",
                    "type": "home",
                    "address1": "<string>",
                    "address2": "<string>",
                    "city": "<string>",
                    "state": "<string>",
                    "country": "US",
                    "zip5": "<string>",
                    "created_at": "2023-11-07T05:31:56Z",
                    "updated_at": "2023-11-07T05:31:56Z"
                  }
                ],
                "contacts": [
                  {
                    "id": "3c90c3cc-0d44-4b50-8888-8dd25736052a",
                    "type": "email",
                    "value": "<string>",
                    "created_at": "2023-11-07T05:31:56Z",
                    "updated_at": "2023-11-07T05:31:56Z"
                  }
                ],
                "status_reasons": [
                  {
                    "reason_code": "invalid_address",
                    "reason_code_message": "Address submitted is a non-supported address type"
                  }
                ],
                "created_at": "2023-11-07T05:31:56Z",
                "updated_at": "2023-11-07T05:31:56Z",
                "closed_at": "2023-11-07T05:31:56Z",
                "metadata": {}
              }
            }
            """.trimIndent()
        )

        // TODO: verify arguments
        val request = OrumCreatePersonRequest(
            customerReferenceId,
            firstName,
            lastName,
            contacts = listOf(),
            socialSecurityNumber = null
        )

        val personResponse = orumUtil.updatePerson(request)
        assertEquals(personId, personResponse.person.id)
        assertEquals(firstName, personResponse.person.firstName)
        assertEquals(lastName, personResponse.person.lastName)
        assertEquals(customerReferenceId, personResponse.person.customerReferenceId)
        assertEquals(dob, personResponse.person.dateOfBirth)
    }

    @Test
    fun `test isCreatePersonAlreadyExistsException returns true`() {
        val exception = OrumException(
            400,
            "{\"error_code\":\"duplicate_customer_reference_id\",\"message\":\"Resource already exists with provided customer reference id. Pass a unique customer reference id.\"}"
        )
        assertTrue(exception.isCreatePersonAlreadyExistsException())
    }

    @Test
    fun `test isCreatePersonAlreadyExistsException returns false on different error code`() {
        val exception = OrumException(404, "{\"error_code\":\"duplicate_customer_reference_id\",\"message\":\"Resource already exists with provided customer reference id. Pass a unique customer reference id.\"}")
        assertFalse(exception.isCreatePersonAlreadyExistsException())
    }

    @Test
    fun `test isCreatePersonAlreadyExistsException returns false on different error message`() {
        val exception = OrumException(404, "{\"error_code\":\"duplicate_orum_reference_id\",\"message\":\"Resource already exists with provided customer reference id. Pass a unique customer reference id.\"}")
        assertFalse(exception.isCreatePersonAlreadyExistsException())
    }

    private fun getMockCredentialsResponse(): String {
        return """
            {
                "access_token": "accessToken",
                "token_type": "tokenType",
                "expires_in": 123
            }
        """.trimIndent()
    }

    private fun mockResponse(response: String, isSuccessful: Boolean = true, code: Int = 200): Call {
        val mockedCall = mockk<Call>()
        val mockedResponse = mockk<Response>()
        val mockedResponseBody = mockk<ResponseBody>()
        every {
            mockedCall.execute()
        } returns mockedResponse
        every {
            mockedResponse.body
        } returns mockedResponseBody
        every {
            mockedResponse.isSuccessful
        } returns isSuccessful
        every {
            mockedResponse.code
        } returns code
        every {
            mockedResponseBody.string()
        } returns response

        return mockedCall
    }
}

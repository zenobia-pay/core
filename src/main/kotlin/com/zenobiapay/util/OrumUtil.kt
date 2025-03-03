package com.zenobiapay.util

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.zenobiapay.model.orum.*
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import kotlin.math.log
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class OrumException(message: String): Exception(message)

class OrumUtil @Inject constructor(
    private val client: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val orumCredentials: OrumCredentials
) {

    companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaTypeOrNull()
        val CUSTOMER_REFERENCE_PREFIX = "sub_"
    }

    fun getAccessToken(orumCredentials: OrumCredentials): OrumTokenResponse {
        val body = objectMapper.writeValueAsString(orumCredentials).toRequestBody(JSON_MEDIA_TYPE)

        logger.info { "Requesting credentials using body $body" }

        val request = Request.Builder()
            .addOrumHeaders()
            .url("https://api-sandbox.orum.io/oauth/token")
            .post(body)
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/json")
            .build()

        return getResponseOrThrowException(OrumTokenResponse::class.java) {
            client.newCall(request).execute()
        }.also {
            logger.info { "Got access token ${it.accessToken}" } // TODO: DEFINITELY REMOVE FROM PROD
        }
    }

    fun createExternalOrganization(createExternalAccountRequest: OrumCreateExternalAccountRequest): OrumCreateExternalAccountResponse {
        val accessToken = getAccessToken(orumCredentials)
        val body = objectMapper.writeValueAsString(createExternalAccountRequest).toRequestBody(JSON_MEDIA_TYPE)
        logger.info { "Creating external organization with request $createExternalAccountRequest" }

        val request = Request.Builder()
            .addOrumHeaders(accessToken.accessToken)
            .url("https://api-sandbox.orum.io/deliver/external/accounts")
            .post(body)
            .build()

        return getResponseOrThrowException(OrumCreateExternalAccountResponse::class.java) {
            client.newCall(request).execute()
        }
    }

    fun createTransfer(createTransferRequest: OrumCreateTransferRequest): OrumCreateTransferResponse {
        val accessToken = getAccessToken(orumCredentials)
        val body = objectMapper.writeValueAsString(
            createTransferRequest.copy(
                source = if (createTransferRequest.source != null) {
                    createTransferRequest.source.copy(
                        customerReferenceId = CUSTOMER_REFERENCE_PREFIX + createTransferRequest.source.customerReferenceId
                    )
                } else {
                    null
                }
            )
        ).toRequestBody(JSON_MEDIA_TYPE)
        logger.info { "Creating transfer with request $createTransferRequest" }
        val request = Request.Builder()
            .addOrumHeaders(accessToken.accessToken)
            .url("https://api-sandbox.orum.io/deliver/transfers")
            .post(body)
            .build()

        val response = getResponseOrThrowException(OrumCreateTransferResponse::class.java) {
            client.newCall(request).execute()
        }
        waitForOrumTransferStatus(response.transfer.id)
        return response
    }

    private fun waitForOrumTransferStatus(transferId: String) {
        logger.info { "Waiting for completion of orum transfer with id $transferId" }
        waitUntilCondition(
            timeout = 10.seconds,
            sleep = 1.seconds,
            successCondition = { response ->
                response.transfer.status.let {
                    it == OrumTransferStatus.PENDING ||
                            it == OrumTransferStatus.SETTLED ||
                            it == OrumTransferStatus.COMPLETED
                }
            },
            failCondition = { response -> response.transfer.status == OrumTransferStatus.FAILED }
        ) {
            getTransfer(transferId).also {
                logger.info { "Got transfer status ${it.transfer.status}" }
            }
        }
    }

    fun getTransfer(transferId: String): OrumGetTransferResponse {
        logger.info { "Fetching transfer with id $transferId" }
        val accessToken = getAccessToken(orumCredentials)
        val request = Request.Builder()
            .addOrumHeaders(accessToken.accessToken)
            .url("https://api-sandbox.orum.io/deliver/transfers/$transferId")
            .build()

        return getResponseOrThrowException(OrumGetTransferResponse::class.java) {
            client.newCall(request).execute()
        }
    }

    fun createPerson(createPersonRequest: OrumCreatePersonRequest): OrumCreatePersonResponse {
        val accessToken = getAccessToken(orumCredentials)
        val body = objectMapper.writeValueAsString(
            createPersonRequest.copy(
                customerReferenceId = CUSTOMER_REFERENCE_PREFIX + createPersonRequest.customerReferenceId
            )
        ).toRequestBody(JSON_MEDIA_TYPE)
        logger.info { "Creating person with request $createPersonRequest" }
        val request = Request.Builder()
            .addOrumHeaders(accessToken.accessToken)
            .url("https://api-sandbox.orum.io/deliver/persons")
            .post(body)
            .build()

        return getResponseOrThrowException(OrumCreatePersonResponse::class.java) {
            client.newCall(request).execute()
        }
    }

    private fun <T> getResponseOrThrowException(responseClass: Class<T>, block: () -> Response): T {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body!!.string()
            logger.info { "Got Orum response $body" } // TODO: maybe remove? Make debug?
            return objectMapper.readValue(body, responseClass)
        }
        throw OrumException("Failed to get response ${responseClass.simpleName}. Error code ${response.code}, body ${response.body?.string()}")
    }
}

fun Request.Builder.addOrumHeaders(accessToken: String? = null): Request.Builder {
    val builder = this.addHeader("accept", "application/json")
        .addHeader("Orum-Version", "v2022-09-21")
        .addHeader("content-type", "application/json")

    if (accessToken != null) {
        return builder.addHeader("authorization", "Bearer $accessToken")
    } else {
        return builder
    }
}

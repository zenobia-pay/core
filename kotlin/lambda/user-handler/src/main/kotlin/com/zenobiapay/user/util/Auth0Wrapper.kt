package com.zenobiapay.user.util

import com.auth0.client.mgmt.ManagementAPI
import com.auth0.json.mgmt.client.Client
import com.auth0.json.mgmt.clientgrants.ClientGrant
import com.auth0.json.mgmt.users.User
import com.auth0.net.Response
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.user.model.Auth0Exception
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class Auth0Wrapper @Inject constructor(private val managementAPI: ManagementAPI) {
    companion object {
        const val ROLE_KEY = "role"
        const val ZENOBIA_AUDIENCE = "https://dashboard.zenobiapay.com"
    }
    fun createClientCredentials(userId: String): Client {
        val client = Client("${userId}#${UUID.randomUUID()}")
        client.description = "M2M Client to act on behalf of merchant $userId"
        client.appType = "non_interactive"
        client.clientMetadata = mapOf(
            "merchantSub" to userId,
            ROLE_KEY to UserPoolGroup.MERCHANT_M2M.value
        )

        val createClientResponse = managementAPI.clients().create(client).execute()
        return getBodyOrThrow(createClientResponse, "Failed to create new m2m client").also {
            logger.info { "Successfully created m2m client with name ${it.name}" }
        }
    }

    fun createClientGrant(client: Client, audience: String): ClientGrant {
        val response = managementAPI.clientGrants().create(client.clientId, audience, arrayOf()).execute()
        return getBodyOrThrow(response, "Failed to create new m2m client").also {
            logger.info { "Successfully added client id ${client.clientId} to audience ${audience}" }
        }
    }

    fun deleteClientCredentials(clientId: String) {
        val response = managementAPI.clients().delete(clientId).execute()
        getBodyOrThrow(response, "Failed to delete m2m client").also {
            logger.info { "Successfully deleted client $clientId" }
        }
    }

    /**
     * Note that this overwrites any existing metadata.
     */
    fun putAppMetadataOnUser(userId: String, metadata: Map<String, String>) {
        val user = getUser(userId)
        val updatedUser = User().apply {
            appMetadata = metadata + (user.appMetadata ?: mapOf<String, String>())
        }
        getBodyOrThrow(managementAPI.users().update(userId, updatedUser).execute(), "Failed to update app metadata")
    }

    private fun getUser(userId: String): User {
        val user = getBodyOrThrow(managementAPI.users().get(userId, null).execute(), "Failed to get auth0 user")
        return user
    }

    private fun <T> getBodyOrThrow(response: Response<T>, message: String): T {
        if (response.statusCode >= 400) { // Auth0 returns 201 instead of 200
            logger.error { "Got error code ${response.statusCode}, ${response.body}"}
            throw Auth0Exception(message)
        }
        return response.body
    }
}
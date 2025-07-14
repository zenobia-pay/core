package com.zenobiapay.user.util

import com.auth0.client.auth.AuthAPI
import com.auth0.client.mgmt.ManagementAPI
import com.auth0.json.mgmt.client.Client
import com.auth0.json.mgmt.clientgrants.ClientGrant
import com.auth0.json.mgmt.users.User
import com.auth0.net.Response
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.user.model.Auth0Exception
import com.zenobiapay.user.model.Auth0ManagementSecret
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Date
import java.util.UUID
import jakarta.inject.Inject
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Data class representing merchant information
 */
data class MerchantInfo(
    val id: String,
    val name: String,
    val approved: Boolean,
    val creationTime: Instant?
)

class Auth0Wrapper @Inject constructor(
    private val authAPI: AuthAPI,
    private val secret: Auth0ManagementSecret,
) {
    companion object {
        const val ROLES_KEY = "roles"
        const val ZENOBIA_AUDIENCE = "https://dashboard.zenobiapay.com"
    }
    fun createClientCredentials(userId: String): Client {
        val client = Client("${userId}#${UUID.randomUUID()}")
        client.description = "M2M Client to act on behalf of merchant $userId"
        client.appType = "non_interactive"
        client.clientMetadata = mapOf(
            "merchantSub" to userId,
            ROLES_KEY to UserPoolGroup.MERCHANT_M2M.value
        )

        val createClientResponse = getManagementApi().clients().create(client).execute()
        return getBodyOrThrow(createClientResponse, "Failed to create new m2m client").also {
            logger.info { "Successfully created m2m client with name ${it.name}" }
        }
    }

    fun createClientGrant(client: Client, audience: String): ClientGrant {
        val response = getManagementApi().clientGrants().create(client.clientId, audience, arrayOf()).execute()
        return getBodyOrThrow(response, "Failed to create new m2m client").also {
            logger.info { "Successfully added client id ${client.clientId} to audience ${audience}" }
        }
    }

    fun deleteClientCredentials(clientId: String) {
        val response = getManagementApi().clients().delete(clientId).execute()
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
        getBodyOrThrow(getManagementApi().users().update(userId, updatedUser).execute(), "Failed to update app metadata")
    }

    private fun getUser(userId: String): User {
        val user = getBodyOrThrow(getManagementApi().users().get(userId, null).execute(), "Failed to get auth0 user")
        return user
    }
    
    /**
     * Lists all users (merchants) with their name, id, and approval status
     * Approval is determined by the presence of the "roles" field in app_metadata
     */
    fun listMerchants(): List<MerchantInfo> {
        val users = getBodyOrThrow(
            getManagementApi().users().list(null).execute(),
            "Failed to list auth0 users"
        )
        
        return users.items.map { user ->
            val appMetadata = user.appMetadata ?: mapOf<String, Any>()
            val isApproved = appMetadata[ROLES_KEY]?.let { it as String }.orEmpty().contains(UserPoolGroup.MERCHANT.value.toString())
            val createdAt = user.createdAt.toInstant()
            
            MerchantInfo(
                id = user.id ?: "",
                name = user.name ?: "",
                approved = isApproved,
                creationTime = createdAt
            )
        }
    }

    fun getManagementApi(): ManagementAPI {
        return ManagementAPI
            .newBuilder(secret.domain, getAuthManagementToken())
            .build()
    }

    private fun getAuthManagementToken(): String {
        logger.info { "Refreshed auth0 token" }
        return authAPI.requestToken(secret.audience).execute().body.accessToken
    }

    private fun <T> getBodyOrThrow(response: Response<T>, message: String): T {
        if (response.statusCode >= 400) { // Auth0 returns 201 instead of 200
            logger.error { "Got error code ${response.statusCode}, ${response.body}"}
            throw Auth0Exception(message)
        }
        return response.body
    }
}
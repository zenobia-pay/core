package com.zenobiapay.user.util

import com.auth0.client.mgmt.ManagementAPI
import com.auth0.json.mgmt.client.Client
import com.auth0.json.mgmt.permissions.Permission
import com.zenobiapay.user.model.Auth0Exception
import com.zenobiapay.user.model.Auth0Permissions
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class Auth0Wrapper @Inject constructor(private val managementAPI: ManagementAPI) {
    fun createClientCredentials(userId: String): Client {
        val client = Client("${userId}_${UUID.randomUUID()}")
        client.description = "M2M Client to act on behalf of merchant $userId"
        client.appType = "non_interactive"
        client.clientMetadata = mapOf("merchantSub" to userId)

        val createClientResponse = managementAPI.clients().create(client).execute()
        if (createClientResponse.statusCode >= 400) { // Auth0 returns 201 instead of 200
            logger.error { "Got error code ${createClientResponse.statusCode}, ${createClientResponse.body}"}
            throw Auth0Exception("Failed to create new m2m client")
        }

        logger.info { "Successfully created m2m client with name ${createClientResponse.body.name}" }
        return createClientResponse.body
    }

    fun putPermissionsToUser(userId: String, permissions: List<Auth0Permissions>) {
        val castPermissions = permissions.map { permission ->
            Permission().also {
                it.name = permission.name
            }
        }
        managementAPI.users().addPermissions(userId, castPermissions)
    }
}
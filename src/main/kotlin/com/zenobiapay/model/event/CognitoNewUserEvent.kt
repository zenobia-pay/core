package com.zenobiapay.model.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper

@JsonIgnoreProperties(ignoreUnknown = true)
data class CognitoNewUserEvent(
    val version: String,
    val region: String,
    val userPoolId: String,
    val userName: String,
    val triggerSource: String,
    val request: CognitoNewUserRequest
) {
    companion object {
        fun from(json: Map<String, Any>, objectMapper: ObjectMapper) =
            objectMapper.convertValue(json, CognitoNewUserEvent::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CognitoNewUserRequest(
    val userAttributes: UserAttributes
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAttributes(
    val sub: String,
    val email: String,
    @JsonProperty("given_name")
    val givenName: String,
    @JsonProperty("family_name")
    val familyName: String
)

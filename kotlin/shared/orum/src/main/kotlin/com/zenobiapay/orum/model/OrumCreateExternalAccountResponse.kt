package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCreateExternalAccountResponse(
    @JsonProperty("external_account")
    val externalAccount: ExternalAccount
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalAccount(
    val id: String,
    @JsonProperty("account_reference_id")
    val accountReferenceId: String,
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("customer_resource_type")
    val customerResourceType: String,
    @JsonProperty("account_type")
    val accountType: String,
    val status: String
)

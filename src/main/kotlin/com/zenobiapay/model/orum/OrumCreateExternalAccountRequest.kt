package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCreateExternalAccountRequest(
    @JsonProperty("account_reference_id")
    val accountReferenceId: String,
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("customer_resource_type")
    val customerResourceType: String,
    @JsonProperty("account_type")
    val accountType: String,
    @JsonProperty("account_number")
    val accountNumber: String,
    @JsonProperty("routing_number")
    val routingNumber: String,
    @JsonProperty("account_holder_name")
    val accountHolderName: String
)

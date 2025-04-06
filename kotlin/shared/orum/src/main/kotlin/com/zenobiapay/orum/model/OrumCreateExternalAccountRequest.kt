package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class OrumCreateExternalAccountRequest(
    @JsonProperty("account_reference_id")
    val accountReferenceId: String,
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("customer_resource_type")
    val customerResourceType: CustomerResourceType,
    @JsonProperty("account_type")
    val accountType: String,
    @JsonProperty("account_number")
    val accountNumber: String,
    @JsonProperty("routing_number")
    val routingNumber: String,
    @JsonProperty("account_holder_name")
    val accountHolderName: String
)

enum class CustomerResourceType(val value: String) {
    BUSINESS("business"),
    PERSON("person"),
    ENTERPRISE("enterprise");

    @JsonValue
    fun toValue(): String = value
}

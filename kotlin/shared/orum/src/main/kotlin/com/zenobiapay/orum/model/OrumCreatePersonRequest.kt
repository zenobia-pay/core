package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCreatePersonRequest(
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("last_name")
    val lastName: String,
    @JsonProperty("social_security_number")
    val socialSecurityNumber: String?,
    val contacts: List<Contact>
)

data class Contact(
    val type: String,
    val value: String
)

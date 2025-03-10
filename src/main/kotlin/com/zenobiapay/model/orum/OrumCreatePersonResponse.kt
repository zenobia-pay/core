package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrumCreatePersonResponse(
    val person: Person
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Person(
    val id: String,
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("middle_name")
    val middleName: String?,
    @JsonProperty("last_name")
    val lastName: String,
    @JsonProperty("date_of_birth")
    val dateOfBirth: String?,
    val status: String,
    @JsonProperty("status_reasons")
    val statusReason: List<StatusReason>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StatusReason(
    @JsonProperty("reason_code")
    val reasonCode: String,
    @JsonProperty("reason_code_message")
    val reasonCodeMessage: String
)

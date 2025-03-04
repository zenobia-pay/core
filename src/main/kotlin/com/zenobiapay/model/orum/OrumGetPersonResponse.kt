package com.zenobiapay.model.orum

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrumGetPersonResponse(
    val person: Person
)

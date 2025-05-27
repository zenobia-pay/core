package com.zenobiapay.rds.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataTableSecret(
    val password: String
)
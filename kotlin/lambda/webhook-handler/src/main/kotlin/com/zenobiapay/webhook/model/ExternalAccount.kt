package com.zenobiapay.webhook.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ExternalAccount(
    val external_account: Account
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    val account_reference_id: String,
    val customer_reference_id: String,
    val customer_resource_type: String,
    val account_type: String,
)

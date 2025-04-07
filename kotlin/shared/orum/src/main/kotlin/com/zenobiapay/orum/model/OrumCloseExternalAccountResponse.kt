package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty

data class OrumCloseExternalAccountResponse(
    @JsonProperty("external_account")
    val externalAccount: ExternalAccount
)

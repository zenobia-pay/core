package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class OrumCreateBusinessResponse(
    val business: OrumCreateBusinessResponseInner
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrumCreateBusinessResponseInner(
    val id: String,
    val status: CreateBusinessStatus,
)

enum class CreateBusinessStatus {
    CREATED,
    VERIFIED,
    REJECTED,
    RESTRICTED,
    CLOSED;

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromJson(value: String): CreateBusinessStatus {
            return entries.first { it.name.uppercase() == value.uppercase() }
        }
    }
}

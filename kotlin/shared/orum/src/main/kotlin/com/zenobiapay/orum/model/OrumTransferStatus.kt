package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.zenobiapay.orum.OrumException

enum class OrumTransferStatus(@JsonValue private val value: String) {
    COMPLETED("completed"),
    CREATED("created"),
    FAILED("failed"),
    PENDING("pending"),
    SETTLED("settled");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromValue(value: String): OrumTransferStatus {
            return entries.find { it.value == value }
                ?: throw OrumException(
                    404,
                    "Could not find matching transfer status for value: $value"
                )
        }
    }
}

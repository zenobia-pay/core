package com.zenobiapay.api.model.cognito

enum class UserPoolGroup(val value: String?) {
    MERCHANT("MERCHANT"),
    CUSTOMER("CUSTOMER"),
    MERCHANT_M2M("MERCHANT_M2M"),
    UNKNOWN(null);

    companion object {
        fun fromString(s: String): UserPoolGroup {
            return entries.find { it.value == s } ?: UNKNOWN
        }
    }
}

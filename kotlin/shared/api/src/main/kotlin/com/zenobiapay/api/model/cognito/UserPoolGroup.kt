package com.zenobiapay.api.model.cognito

enum class UserPoolGroup(val value: String) {
    MERCHANT("MerchantGroup"),
    CUSTOMER("CustomerGroup");

    companion object {
        fun fromString(s: String): UserPoolGroup? {
            return entries.find { it.value == s }
        }
    }
}

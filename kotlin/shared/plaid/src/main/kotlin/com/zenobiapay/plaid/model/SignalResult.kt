package com.zenobiapay.plaid.model

enum class SignalResult {
    ACCEPT,
    DENY;

    companion object {
        fun getResult(customAction: String?): SignalResult {
            return if (customAction == "accept") {
                ACCEPT
            } else {
                DENY
            }
        }
    }
}
package com.zenobiapay.plaid.model

import com.plaid.client.model.RuleResult

enum class SignalResult {
    ACCEPT,
    WAIT,
    DENY;

    companion object {
        fun getResult(outcome: RuleResult?): SignalResult {
            return when (outcome) {
                RuleResult.ACCEPT -> ACCEPT
                RuleResult.REROUTE -> DENY
                RuleResult.REVIEW -> WAIT
                RuleResult.ENUM_UNKNOWN, null -> DENY
            }
        }
    }
}
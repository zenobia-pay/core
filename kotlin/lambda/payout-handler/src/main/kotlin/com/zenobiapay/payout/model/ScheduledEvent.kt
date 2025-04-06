package com.zenobiapay.payout.model

data class ScheduledEvent(
    val version: String,
    val id: String,
    val detailType: String,
    val source: String,
    val account: String,
    val time: String,
    val region: String,
    val resources: List<String>,
    val detail: Map<String, Any>
)
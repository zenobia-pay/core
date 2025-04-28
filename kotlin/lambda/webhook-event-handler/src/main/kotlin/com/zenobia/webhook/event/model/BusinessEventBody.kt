package com.zenobia.webhook.event.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class BusinessEventBody(
    val business: Business
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Business(
    val customer_reference_id: String,
    val business_name: String?,
    val entity_name: String?,
)

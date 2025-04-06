package com.zenobiapay.orum.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class OrumCreateBusinessRequest(
    @JsonProperty("customer_reference_id")
    val customerReferenceId: String,
    @JsonProperty("legal_name")
    val legalName: String,
    @JsonProperty("business_name")
    val businessName: String? = null,
    @JsonProperty("entity_type")
    val entityType: BusinessEntityType? = null,
    @JsonProperty("tax_id")
    val taxId: String? = null,
    @JsonProperty("tax_id_type")
    val taxIdType: TaxIdType? = null,
    @JsonProperty("account_holder_name")
    val accountHolderName: String? = null,
    @JsonProperty("incorporation_date")
    val incorporationDate: String? = null,
    val addresses: List<Address>? = null,
    val contacts: List<Contact>? = null,
)

enum class BusinessEntityType(val value: String) {
    SOLE_PROPRIETORSHIP("sole_proprietorship"),
    PARTNERSHIP("partnership"),
    LLP("limited_liability_partnership"),
    LLC("limited_liability_company"),
    C_CORP("c_corporation"),
    S_CORP("s_corporation"),
    B_CORP("b_corporation"),
    NON_PROFIT("nonprofit_corporation");

    @JsonValue
    fun toJson(): String = value
}

enum class TaxIdType(val value: String) {
    EIN("ein"),
    TIN("tin");

    @JsonValue
    fun toJson(): String = value
}

data class Address(
    val type: String = "legal",
    val address1: String,
    val address2: String?,
    val city: String,
    val state: String,
    val country: String,
    val zip5: String,
)

package com.zenobiapay.util

import com.plaid.client.model.*
import com.plaid.client.request.PlaidApi
import io.github.oshai.kotlinlogging.KotlinLogging
import retrofit2.Response
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class PlaidException(message: String) : Exception(message)

class PlaidUtil @Inject constructor(private val plaidApi: PlaidApi) {
    fun createLinkToken(userId: String): LinkTokenCreateResponse {
        val user = LinkTokenCreateRequestUser()
            .clientUserId(userId)

        // TODO: I think redirectUri should ONLY be passed if the request is from IOS
        val request = LinkTokenCreateRequest()
            .user(user)
            .clientName("Zenobia")
            .products(listOf(Products.AUTH))
            .countryCodes((listOf(CountryCode.US)))
            .language("en")
            .redirectUri("https://zenobiapay.com/plaid")

        return getResponseOrThrowException("CreateLinkToken") {
            plaidApi.linkTokenCreate(request).execute()
        }
    }

    fun exchangeLinkToken(userId: String, linkToken: String): ItemPublicTokenExchangeResponse {
        val request = ItemPublicTokenExchangeRequest()
            .clientId(userId)
            .publicToken(linkToken)
        return getResponseOrThrowException("ExchangeLinkToken") {
            plaidApi.itemPublicTokenExchange(request).execute()
        }
    }

    fun getItem(userId: String, accessToken: String): ItemGetResponse {
        val request = ItemGetRequest()
            .clientId(userId)
            .accessToken(accessToken)
        return getResponseOrThrowException("GetItem") {
            plaidApi.itemGet(request).execute()
        }
    }

    fun getZippedAccountsAndAch(accessToken: String): List<Pair<AccountBase, NumbersACH?>> {
        val accounts = getAccounts(accessToken).accounts
        val achNumbers = getAuth(accessToken).numbers.ach

        return accounts.map { account ->
            val achNumber = achNumbers.find {
                (it.accountId == account.accountId)
            }
            account to achNumber
        }
    }

    fun getAccounts(accessToken: String): AccountsGetResponse {
        val request = AccountsGetRequest()
            .accessToken(accessToken)

        return getResponseOrThrowException("AccountsGet") {
            plaidApi.accountsGet(request).execute()
        }
    }

    fun getAuth(accessToken: String): AuthGetResponse {
        val request = AuthGetRequest()
            .accessToken(accessToken)
        return getResponseOrThrowException("AuthGet") {
            plaidApi.authGet(request).execute()
        }.also {
            logger.info { "got plaid response for ach ${it.numbers.ach}" }
        }
    }

//    fun createTransferAuthorization() {
//        val request = TransferAuthorizationCreateRequest()
//            .
//        plaidApi.transferAuthorizationCreate()
//    }

    private fun <T> getResponseOrThrowException(operationName: String, block: () -> Response<T>): T {
        val response = block()
        if (response.isSuccessful) {
            return response.body()!!
        }
        throw PlaidException("Failed to call $operationName. Error code ${response.code()}, body ${response.errorBody()?.string()}")
    }
}

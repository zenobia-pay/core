package com.zenobiapay.plaid

import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountsGetRequest
import com.plaid.client.model.AccountsGetResponse
import com.plaid.client.model.AuthGetRequest
import com.plaid.client.model.AuthGetResponse
import com.plaid.client.model.CountryCode
import com.plaid.client.model.IdentityVerification
import com.plaid.client.model.IdentityVerificationGetRequest
import com.plaid.client.model.IdentityVerificationGetResponse
import com.plaid.client.model.ItemGetRequest
import com.plaid.client.model.ItemGetResponse
import com.plaid.client.model.ItemPublicTokenExchangeRequest
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.ItemRemoveRequest
import com.plaid.client.model.ItemRemoveResponse
import com.plaid.client.model.LinkTokenCreateRequest
import com.plaid.client.model.LinkTokenCreateRequestIdentityVerification
import com.plaid.client.model.LinkTokenCreateRequestUser
import com.plaid.client.model.LinkTokenCreateResponse
import com.plaid.client.model.NumbersACH
import com.plaid.client.model.Products
import com.plaid.client.request.PlaidApi
import io.github.oshai.kotlinlogging.KotlinLogging
import retrofit2.Response
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

class PlaidException(message: String) : Exception(message)

class PlaidWrapper @Inject constructor(private val plaidApi: PlaidApi) {
    fun createLinkToken(userId: String, product: Products): LinkTokenCreateResponse {
        val user = LinkTokenCreateRequestUser()
            .clientUserId(userId)

        // TODO: I think redirectUri should ONLY be passed if the request is from IOS
        var request = LinkTokenCreateRequest()
            .user(user)
            .clientName("Zenobia")
            .products(listOf(product))
            .countryCodes((listOf(CountryCode.US)))
            .language("en")
            .redirectUri("https://zenobiapay.com/plaid")

        if (product == Products.IDENTITY_VERIFICATION) {
            request = request.identityVerification(
                LinkTokenCreateRequestIdentityVerification()
                    .templateId("idvtmp_aMnDHwUDRwYP2s") // TODO: make env var
            )
        }

        return getResponseOrThrowException("CreateLinkToken") {
            plaidApi.linkTokenCreate(request).execute()
        }
    }

    fun exchangeLinkToken(linkToken: String): ItemPublicTokenExchangeResponse {
        val request = ItemPublicTokenExchangeRequest()
            .publicToken(linkToken)
        return getResponseOrThrowException("ExchangeLinkToken") {
            plaidApi.itemPublicTokenExchange(request).execute()
        }
    }

    fun getItem(accessToken: String): ItemGetResponse {
        val request = ItemGetRequest()
            .accessToken(accessToken)
        return getResponseOrThrowException("GetItem") {
            plaidApi.itemGet(request).execute()
        }
    }

    fun removeItem(accessToken: String): ItemRemoveResponse {
        val request = ItemRemoveRequest()
            .accessToken(accessToken)
        return getResponseOrThrowException("RemoveItem") {
            plaidApi.itemRemove(request).execute()
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
        }
    }

    fun getIdentityVerification(identityVerificationId: String): IdentityVerificationGetResponse {
        val request = IdentityVerificationGetRequest()
            .identityVerificationId(identityVerificationId)
        return getResponseOrThrowException("GetIdentityVerification") {
            plaidApi.identityVerificationGet(request).execute()
        }
    }

    private fun <T> getResponseOrThrowException(operationName: String, block: () -> Response<T>): T {
        val response = block()
        if (response.isSuccessful) {
            return response.body()!!
        }
        throw PlaidException("Failed to call $operationName. Error code ${response.code()}, body ${response.errorBody()?.string()}")
    }
}

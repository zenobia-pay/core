package com.zenobiapay.plaid

import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountsBalanceGetRequest
import com.plaid.client.model.AccountsGetRequest
import com.plaid.client.model.AccountsGetResponse
import com.plaid.client.model.AuthGetRequest
import com.plaid.client.model.AuthGetResponse
import com.plaid.client.model.CountryCode
import com.plaid.client.model.DepositoryAccountSubtype
import com.plaid.client.model.DepositoryFilter
import com.plaid.client.model.IdentityGetRequest
import com.plaid.client.model.IdentityGetResponse
import com.plaid.client.model.IdentityVerificationGetRequest
import com.plaid.client.model.IdentityVerificationGetResponse
import com.plaid.client.model.ItemGetRequest
import com.plaid.client.model.ItemGetResponse
import com.plaid.client.model.ItemPublicTokenExchangeRequest
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.ItemRemoveRequest
import com.plaid.client.model.ItemRemoveResponse
import com.plaid.client.model.LinkTokenAccountFilters
import com.plaid.client.model.LinkTokenCreateRequest
import com.plaid.client.model.LinkTokenCreateRequestUser
import com.plaid.client.model.LinkTokenCreateResponse
import com.plaid.client.model.NumbersACH
import com.plaid.client.model.Products
import com.plaid.client.model.SignalEvaluateRequest
import com.plaid.client.model.WebhookVerificationKeyGetRequest
import com.plaid.client.model.WebhookVerificationKeyGetResponse
import com.plaid.client.request.PlaidApi
import com.zenobia.metric.MetricHelper
import com.zenobiapay.plaid.di.IS_PLAID_SANDBOX
import com.zenobiapay.plaid.model.SignalResult
import io.github.oshai.kotlinlogging.KotlinLogging
import retrofit2.Response
import jakarta.inject.Inject
import jakarta.inject.Named
import kotlin.math.floor

private val logger = KotlinLogging.logger {}

open class PlaidException(message: String) : Exception(message)
class PlaidBankAccountNotFoundException(): PlaidException("Could not find bank account")

class PlaidWrapper @Inject constructor(
    private val plaidApi: PlaidApi,
    @Named(IS_PLAID_SANDBOX)
    private val isPlaidSandbox: Boolean,
) {
    fun createLinkToken(userId: String, webhookUrl: String, product: List<Products>): LinkTokenCreateResponse {
        val user = LinkTokenCreateRequestUser()
            .clientUserId(userId)

        val accountFilters = LinkTokenAccountFilters().depository(
            DepositoryFilter().addAccountSubtypesItem(DepositoryAccountSubtype.CHECKING)
        )
        // TODO: I think redirectUri should ONLY be passed if the request is from IOS
        var request = LinkTokenCreateRequest()
            .user(user)
            .clientName("Zenobia")
            .products(product)
            .countryCodes((listOf(CountryCode.US)))
            .language("en")
            .accountFilters(accountFilters)
            .webhook(webhookUrl)
            .redirectUri("https://zenobiapay.com/plaid")

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

    fun getIdentity(accessToken: String): IdentityGetResponse {
        val request = IdentityGetRequest().accessToken(accessToken)
        return getResponseOrThrowException("IdentityGet") {
            plaidApi.identityGet(request).execute()
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

    fun getAvailableBalance(accessToken: String, accountId: String): Int {
        val request = AccountsBalanceGetRequest()
            .accessToken(accessToken);

        val response = getResponseOrThrowException("AccountsBalanceGet") {
            plaidApi.accountsBalanceGet(request).execute()
        }
        val matchingAccount = response.accounts.firstOrNull {
            it.accountId == accountId
        } ?: throw PlaidBankAccountNotFoundException()

        val balance = matchingAccount.balances.available ?: matchingAccount.balances.current!!
        return floor(balance * 100).toInt()
    }

    fun getRiskDecision(accessToken: String, accountId: String, requestId: String, amount: Int, userId: String): SignalResult {
//        if (isPlaidSandbox) {
//            logger.info { "In sandbox. Return ACCEPT." }
//            return SignalResult.ACCEPT
//        }
        val request = SignalEvaluateRequest()
            .accessToken(accessToken)
            .accountId(accountId)
            .clientTransactionId(requestId)
            .amount(amount / 100.0)
            .clientUserId(userId)
            .defaultPaymentMethod("SAME_DAY_ACH")
            .rulesetKey("zenobia-risk-rules")

        val response = getResponseOrThrowException("SignalEvaluate") {
            plaidApi.signalEvaluate(request).execute()
        }
        logger.info { "Got response $response" }
        logger.info { "ruleset: ${response.ruleset}" }
        logger.info { "Triggered rule details: ${response.ruleset?.triggeredRuleDetails}" }
        logger.info { "Result: ${response.ruleset?.triggeredRuleDetails?.result}" }
        return SignalResult.getResult(response.ruleset?.triggeredRuleDetails?.result)
    }

    fun getWebhookVerificationKey(keyId: String): WebhookVerificationKeyGetResponse {
        val request = WebhookVerificationKeyGetRequest()
            .keyId(keyId)
        return getResponseOrThrowException("WebhookVerificationKeyGet") {
            plaidApi.webhookVerificationKeyGet(request).execute()
        }
    }

    private fun <T> getResponseOrThrowException(operationName: String, block: () -> Response<T>): T {
        return MetricHelper.withXray("Plaid.$operationName") {
            val response = block()
            if (response.isSuccessful) {
                return@withXray response.body()!!
            }
            throw PlaidException("Failed to call $operationName. Error code ${response.code()}, body ${response.errorBody()?.string()}")
        }
    }
}

package com.zenobiapay.bank.operations

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.plaid.client.model.AccountBase
import com.plaid.client.model.AccountSubtype
import com.plaid.client.model.Email
import com.plaid.client.model.IdentityGetResponse
import com.plaid.client.model.ItemPublicTokenExchangeResponse
import com.plaid.client.model.NumbersACH
import com.plaid.client.model.Owner
import com.plaid.client.model.PhoneNumber
import com.zenobiapay.api.generated.model.ExchangeToken200Response
import com.zenobiapay.table.bank.dao.BankDao
import com.zenobiapay.api.model.exception.InvalidRequestException
import com.zenobiapay.api.generated.model.ExchangeTokenRequest
import com.zenobiapay.api.operation.Operation
import com.zenobiapay.api.model.cognito.UserPoolGroup
import com.zenobiapay.api.model.exception.DetailsNeededException
import com.zenobiapay.api.util.getUserRole
import com.zenobiapay.cryptography.util.isCertificateValid
import com.zenobiapay.orum.OrumWrapper
import com.zenobiapay.orum.model.Contact
import com.zenobiapay.orum.model.CustomerResourceType
import com.zenobiapay.orum.model.OrumCreateExternalAccountRequest
import com.zenobiapay.orum.model.OrumCreatePersonRequest
import com.zenobiapay.orum.util.generateCustomerOrumId
import com.zenobiapay.orum.util.generateMerchantOrumId
import com.zenobiapay.plaid.PlaidWrapper
import com.zenobiapay.table.bank.model.DeviceCertificate
import com.zenobiapay.table.credentials.dao.CredentialsDao
import com.zenobiapay.table.user.dao.UserDao
import com.zenobiapay.table.user.model.UserType
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import jakarta.inject.Inject

private val logger = KotlinLogging.logger {}

class ExchangeTokenOperation @Inject constructor(
    private val plaidWrapper: PlaidWrapper,
    private val orumWrapper: OrumWrapper,
    private val bankDao: BankDao,
    private val userDao: UserDao,
    private val credentialsDao: CredentialsDao,
) : Operation<ExchangeTokenRequest, ExchangeToken200Response>() {

    override val inputType = ExchangeTokenRequest::class.java

    override fun run(request: ExchangeTokenRequest, input: APIGatewayProxyRequestEvent, context: Context, userId: String?): ExchangeToken200Response {
        logger.info { "Got input body ${input.body}" }

        if (request.deviceCertificate != null && !isCertificateValid(
                request.deviceCertificate!!.certificateValue,
                request.deviceCertificate!!.certificateType
            )
        ) {
            throw InvalidRequestException("Device Certificate is invalid")
        }

        val exchangeResponse = plaidWrapper.exchangeLinkToken(request.linkToken)
        val sub = (userId ?: request.sub).also {
            logger.info { "Using sub $it" }
        }

        val identityResponse = plaidWrapper.getIdentity(exchangeResponse.accessToken)
        val owner = getOwner(identityResponse)
        var refreshToken = if (input.requestContext.getUserRole() == UserPoolGroup.UNKNOWN) {
            registerCustomer(owner, sub)
            credentialsDao.createRefreshToken(sub!!)
        } else null

        val accountsToAch = plaidWrapper.getZippedAccountsAndAch(exchangeResponse.accessToken)
        accountsToAch.forEach { (account, ach) -> processAccount(
                request = request,
                exchangeResponse = exchangeResponse,
                sub = sub,
                userFullName = owner.names.first(),
                userPoolGroup = input.requestContext.getUserRole(),
                account = account,
                ach = ach
            )
        }

        return ExchangeToken200Response()
            .refreshToken(refreshToken)
            .sub(sub)
    }

    private fun registerCustomer(owner: Owner, sub: String?) {
        if (sub == null) throw InvalidRequestException("Invalid sub used")
        val temporaryUserItem = userDao.getUserItem(sub)
        if (temporaryUserItem == null || temporaryUserItem.ttl == null || temporaryUserItem.data.isApproved) {
            logger.info { "Sub $sub is invalid. User item either doesn't exist or is already in use." }
            throw InvalidRequestException("Invalid sub used.")
        }

        logger.info { "Exchange is successful. Updating sub $sub to be permanent"}
        val (firstName, lastName) = getFirstLastName(owner.names.first())
        try {
            userDao.updateTemporaryCustomerToPermanent(
                sub,
                firstName,
                lastName,
                generateCustomerOrumId(sub),
                UserType.CUSTOMER,
                isApproved = true
            )
        } catch (e: ConditionalCheckFailedException) {
            throw InvalidRequestException("Invalid sub used") // Keep response the same to prevent attackers knowing valid subs
        }
        val createPersonRequest = OrumCreatePersonRequest(
            customerReferenceId = generateCustomerOrumId(sub),
            firstName = firstName,
            lastName = lastName,
            socialSecurityNumber = null,
            contacts = getContacts(owner.emails, listOf())
        )
        logger.info { "Creating orum person" }
        orumWrapper.createPerson(createPersonRequest)
    }

    private fun getOwner(identityGetResponse: IdentityGetResponse): Owner {
        val ownerNotEmptyNames = identityGetResponse.accounts.filter {
            it.owners.isNotEmpty()
        }

        if (ownerNotEmptyNames.isEmpty()) throw DetailsNeededException("Owner names not found.")
        logger.info { "Got ${ownerNotEmptyNames.size} number of accounts"}
        val owner = ownerNotEmptyNames.first().owners.first()
        return owner
    }

    private fun getContacts(emails: List<Email>, phoneNumbers: List<PhoneNumber>): List<Contact> {
        val contacts = mutableListOf<Contact>()
        if (emails.isNotEmpty() == true) {
            contacts.add(Contact(type = "email", value = emails.first().data))
        }
        if (phoneNumbers.isNotEmpty() == true) {
            contacts.add(Contact(type = "phone", value = phoneNumbers.first().data))
        }
        if (contacts.isEmpty()) throw DetailsNeededException("Contact info not found.")
        return contacts
    }

    // TODO: expand to handle more cases
    private fun getFirstLastName(fullName: String): Pair<String, String> {
        if (fullName.contains(",")) {
            val (lastName, firstName) = fullName.split(",", limit = 2)
            return firstName.trim() to lastName.trim()
        }
        val splitName = fullName.split(" ", limit = 2)
        if (splitName.size == 1) return fullName to ""

        return splitName[0].trim() to splitName[1].trim()
    }

    private fun processAccount(
        request: ExchangeTokenRequest,
        exchangeResponse: ItemPublicTokenExchangeResponse,
        sub: String,
        userFullName: String,
        userPoolGroup: UserPoolGroup,
        account: AccountBase,
        ach: NumbersACH?
    ) {
        logger.info { "Processing account $account, ach $ach" }
        if (ach == null) {
            logger.error { "Could not find ach number for account ${account.accountId}" }
            throw InvalidRequestException("Could not find ach number for provided account")
        }
        if (account.subtype != AccountSubtype.CHECKING && account.subtype != AccountSubtype.SAVINGS) {
            logger.error { "expected checking or savings, got subtype ${account.subtype} for account ${account.accountId}" }
            throw InvalidRequestException("Provided account is not checking nor savings")
        }
        val orumId = orumWrapper.createExternalOrganization(
            OrumCreateExternalAccountRequest(
                accountReferenceId = ach.accountId,
                customerReferenceId = getOrumCustomerId(sub, userPoolGroup),
                customerResourceType = getCustomerResourceType(userPoolGroup),
                accountType = account.subtype!!.value,
                accountNumber = ach.account,
                routingNumber = ach.routing,
                accountHolderName = userFullName
            )
        ).externalAccount.id

        val deviceCertificate = if (request.deviceCertificate != null) {
            DeviceCertificate(
                certificateType = request.deviceCertificate!!.certificateType!!.value,
                certificateValue = request.deviceCertificate!!.certificateValue!!,
            )
        } else null

        bankDao.putBankAccount(
            userId = sub,
            deviceId = request.deviceId,
            lastFourDigits = account.mask ?: "****",
            plaidItemId = exchangeResponse.itemId,
            bankAccountId = account.accountId,
            bankAccountName = account.name,
            token = exchangeResponse.accessToken,
            bankAccountType = account.subtype!!.value,
            orumId = orumId,
            deviceCertificate = deviceCertificate,
        )
        logger.info { "Successfully wrote to ddb bank item ${account.accountId}, orum id $orumId" }
    }

    private fun getCustomerResourceType(userPoolGroup: UserPoolGroup): CustomerResourceType {
        return when (userPoolGroup) {
            UserPoolGroup.MERCHANT -> CustomerResourceType.BUSINESS
            UserPoolGroup.CUSTOMER, UserPoolGroup.UNKNOWN -> CustomerResourceType.PERSON
            UserPoolGroup.MERCHANT_M2M  -> throw Exception("Got invalid user pool group $userPoolGroup")
        }
    }

    private fun getOrumCustomerId(userId: String, userPoolGroup: UserPoolGroup): String {
        return when (userPoolGroup) {
            UserPoolGroup.MERCHANT -> generateMerchantOrumId(userId)
            UserPoolGroup.CUSTOMER, UserPoolGroup.UNKNOWN -> generateCustomerOrumId(userId)
            UserPoolGroup.MERCHANT_M2M -> throw Exception("Got invalid user pool group $userPoolGroup")
        }
    }

    override fun getUserPoolAllowList(): List<UserPoolGroup> {
        return listOf(UserPoolGroup.UNKNOWN, UserPoolGroup.CUSTOMER, UserPoolGroup.MERCHANT)
    }
}

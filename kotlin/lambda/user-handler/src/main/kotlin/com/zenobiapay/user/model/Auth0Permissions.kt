package com.zenobiapay.user.model

import com.zenobiapay.api.generated.models.UserType

enum class Auth0Permissions {
    MERCHANT,
    CUSTOMER,
    REQUEST_ONLY;

    companion object {
        fun fromUserType(userType: UserType): Auth0Permissions {
            return when (userType) {
                UserType.MERCHANT -> MERCHANT
                UserType.CUSTOMER -> CUSTOMER
            }
        }
    }
}
package com.zenobiapay.util

import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import at.favre.lib.crypto.bcrypt.BCrypt


class OAuthUtil @Inject constructor() {
    fun generateClientId(): String {
        val bytes = ByteArray(16) // 128-bit ID
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun generateClientSecret(): String {
        val bytes = ByteArray(32) // 256-bit secret
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(value: String): String {
        return BCrypt.withDefaults().hashToString(12, value.toCharArray())
    }

    fun verifyHash(value: String, hashedValue: String): Boolean {
        return BCrypt.verifyer().verify(value.toCharArray(), hashedValue.toCharArray()).verified
    }
}
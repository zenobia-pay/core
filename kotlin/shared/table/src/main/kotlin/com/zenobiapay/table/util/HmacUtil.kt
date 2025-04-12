package com.zenobiapay.table.util

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun signHmacSha256(payload: String, secret: String): String {
    val algorithm = "HmacSHA256"
    val secretKeySpec = SecretKeySpec(secret.toByteArray(), algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(secretKeySpec)
    val hash = mac.doFinal(payload.toByteArray())
    return Base64.getEncoder().encodeToString(hash)
}

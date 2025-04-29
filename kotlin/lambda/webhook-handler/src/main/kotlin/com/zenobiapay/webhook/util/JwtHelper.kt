package com.zenobiapay.webhook.util

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.Base64

fun decodeJwt(base64Jwt: String): DecodedJWT {
    return JWT.decode(base64Jwt)
}

fun verifyPlaidJwt(jwt: String, x: String, y: String): DecodedJWT {
    val publicKey = createEcPublicKey(x, y)

    val algorithm = Algorithm.ECDSA256(publicKey, null)
    val verifier: JWTVerifier = JWT.require(algorithm)
        .build()

    return verifier.verify(jwt)
}

private fun createEcPublicKey(xB64Url: String, yB64Url: String): ECPublicKey {
    val xBytes = Base64.getUrlDecoder().decode(xB64Url)
    val yBytes = Base64.getUrlDecoder().decode(yB64Url)
    val point = ECPoint(
        BigInteger(1, xBytes),
        BigInteger(1, yBytes)
    )

    val params = AlgorithmParameters.getInstance("EC")
    params.init(ECGenParameterSpec("secp256r1"))
    val parameterSpec = params.getParameterSpec(ECParameterSpec::class.java)
    val keySpec = ECPublicKeySpec(point, parameterSpec)
    val keyFactory = KeyFactory.getInstance("EC")

    return keyFactory.generatePublic(keySpec) as ECPublicKey
}

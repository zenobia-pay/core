package com.zenobiapay.cryptography.util

import com.zenobiapay.api.generated.model.CertificateType
import com.zenobiapay.api.generated.model.SignatureType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import java.io.StringReader
import java.lang.Exception
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.util.Base64

private val logger = KotlinLogging.logger {}

fun isCertificateValid(
    certificate: String,
    certificateType: CertificateType
): Boolean {
    Security.addProvider(BouncyCastleProvider())
    try {
        val publicKey = getPublicKey(certificate)
        return publicKey?.algorithm == certificateType.value
    } catch (e: Exception) {
        logger.info { "Caught error when parsing certificate, returning false to customer"}
        return false
    }
}

fun isSignatureValid(
    data: ByteArray,
    certificate: String,
    base64Signature: String,
    signatureType: SignatureType,
): Boolean {
    return when (signatureType) {
        SignatureType.SHA256_WITH_ECDSA -> isSha256WithEcdsaSignatureValid(data, certificate, base64Signature)
    }
}

private fun isSha256WithEcdsaSignatureValid(
    data: ByteArray,
    certificate: String,
    base64Signature: String,
): Boolean {
    val publicKey = getPublicKey(certificate)

    val verifier = Signature.getInstance("SHA256withECDSA", "BC").apply {
        initVerify(publicKey)
        update(data)
    }

    return verifier.verify(Base64.getDecoder().decode(base64Signature))
}

private fun getPublicKey(certificate: String): PublicKey? {
    Security.addProvider(BouncyCastleProvider())

    val reader = PEMParser(StringReader(certificate))
    val publicKeyInfo = reader.readObject() as SubjectPublicKeyInfo
    return BouncyCastleProvider.getPublicKey(publicKeyInfo)
}
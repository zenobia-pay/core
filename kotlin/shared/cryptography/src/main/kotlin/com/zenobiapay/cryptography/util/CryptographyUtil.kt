package com.zenobiapay.cryptography.util

import com.zenobiapay.api.generated.models.CertificateType
import com.zenobiapay.api.generated.models.SignatureType
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import java.io.StringReader
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.util.Base64

fun isCertificateValid(
    certificate: String,
    certificateType: CertificateType
): Boolean {
    val publicKey = getPublicKey(certificate)
    return publicKey.algorithm == certificateType.value
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
    Security.addProvider(BouncyCastleProvider())

    val publicKey = getPublicKey(certificate)

    val verifier = Signature.getInstance("SHA256withECDSA", "BC").apply {
        initVerify(publicKey)
        update(data)
    }

    return verifier.verify(Base64.getDecoder().decode(base64Signature))
}

private fun getPublicKey(certificate: String): PublicKey {
    val reader = PEMParser(StringReader(certificate))
    val publicKeyInfo = reader.readObject() as SubjectPublicKeyInfo
    return BouncyCastleProvider.getPublicKey(publicKeyInfo)
}
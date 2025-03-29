package com.zenobiapay.transfer.util

import com.zenobiapay.api.generated.models.CertificateType
import com.zenobiapay.api.generated.models.SignatureType
import java.io.ByteArrayInputStream
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

fun validateSignature(
    data: ByteArray,
    certificateValue: String,
    certificateType: CertificateType,
    signatureValue: String,
    signatureType: SignatureType,
): Boolean {
    return when (signatureType) {
        SignatureType.SHA256_WITH_ECDSA -> validateSha256WithEcdsa(data, certificateValue, certificateType, signatureValue)
    }
}

private fun validateSha256WithEcdsa(
    data: ByteArray,
    certificateValue: String,
    certificateType: CertificateType,
    signatureValue: String,
): Boolean {
    val certBytes = Base64.getDecoder().decode(certificateValue)
    val certFactory = CertificateFactory.getInstance("X.509")
    val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
    if (cert.publicKey.algorithm != certificateType.value) {
        return false
    }
    val publicKey = cert.publicKey

    val signature = Signature.getInstance("SHA256withECDSA")
    signature.initVerify(publicKey)
    signature.update(data)

    val sigBytes = Base64.getDecoder().decode(signatureValue)
    return signature.verify(sigBytes)
}

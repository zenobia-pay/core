package com.zenobiapay.bank.util

import com.zenobiapay.api.generated.models.CertificateType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

private val logger = KotlinLogging.logger {}

fun isValidCertificate(certificateValue: String, certificateType: CertificateType): Boolean {
    val certFactory = CertificateFactory.getInstance("X.509")
    try {
        val certBytes = Base64.getDecoder().decode(certificateValue)
        val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
        return cert.publicKey.algorithm == certificateType.value
    } catch (e: CertificateException) {
        logger.info { "Could not parse cert" }
        return false
    }
}

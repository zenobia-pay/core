package com.zenobiapay.cryptography.util

import com.zenobiapay.api.generated.model.CertificateType
import com.zenobiapay.api.generated.model.SignatureType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CryptographyUtilTest {
    val certificate = """
        -----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEt648E1GU9txX89Iso69ydeN5A3PI
        KkaoJp+M2fZEcz3pWunSI7OsbZ/qTwCdI4ZugW1QxCvS1rc2xiB1TajnOg==
        -----END PUBLIC KEY-----
        """.trimIndent()
    val signature = "MEUCIQDbGzhiy/h5nh16qtWz8pf/HT3Ph96ZVfxq24FK99uz+QIgTxrUK9KMgGausbGx8xQmwP7Gvq4cTaRSuMkfH53bJKA="

    @Test
    fun `validates EC certificate`() {
        assertTrue(isCertificateValid(certificate, CertificateType.EC))
    }

    @Test
    fun `doesnt validate non certificate`() {
        assertFalse(isCertificateValid("badCertificate", CertificateType.EC))
    }

    @Test
    fun `validate ECDSA signature success`() {
        val data = "test\n".toByteArray()
        assertTrue(isSignatureValid(data, certificate, signature, SignatureType.SHA256_WITH_ECDSA))
    }

    @Test
    fun `validate invalid ECDSA signature fails`() {
        val data = "testDifferent".toByteArray()
        assertFalse(isSignatureValid(data, certificate, signature, SignatureType.SHA256_WITH_ECDSA))
    }
}
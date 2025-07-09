package com.zenobiapay.cryptography.util

import com.zenobiapay.api.generated.model.CertificateType
import com.zenobiapay.api.generated.model.SignatureType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import java.io.StringReader
import java.lang.Exception
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Security
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

private val logger = KotlinLogging.logger {}

fun isCertificateValid(
    certificate: String,
    certificateType: CertificateType
): Boolean {
    Security.addProvider(BouncyCastleProvider())
    try {
        val publicKey = getPublicKey(certificate)
        logger.info { "Public key: $publicKey, algorithm: ${publicKey?.algorithm}" }
        
        if (publicKey == null) {
            logger.error { "Failed to parse certificate - public key is null" }
            return false
        }
        
        val algorithm = publicKey.algorithm.uppercase()
        val typeValue = certificateType.value.uppercase()
        
        logger.info { "Comparing algorithm '$algorithm' with certificate type '$typeValue'" }
        
        // For EC certificates, be more flexible with the algorithm name
        val isValid = when (typeValue) {
            "EC" -> algorithm.contains("EC") || algorithm == "ECDSA"
            else -> algorithm == typeValue
        }
        
        logger.info { "Certificate validation result: $isValid" }
        return isValid
    } catch (e: Exception) {
        logger.error(e) { "Caught error when validating certificate, returning false to customer"}
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
    
    try {
        logger.info { "Parsing certificate with KeyFactory" }
        
        // Clean up the certificate string
        val cleanCert = certificate.trim()
        
        if (cleanCert.contains("BEGIN PUBLIC KEY")) {
            // Extract the base64 content between BEGIN and END markers
            val pemContent = cleanCert
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\r", "")
                .replace("\n", "")
                .trim()
            
            logger.info { "Extracted PEM content" }
            
            // Decode the base64 content
            val decoded = Base64.getDecoder().decode(pemContent)
            
            // Try with standard JDK KeyFactory first
            try {
                val keyFactory = KeyFactory.getInstance("EC")
                val keySpec = X509EncodedKeySpec(decoded)
                val publicKey = keyFactory.generatePublic(keySpec)
                logger.info { "Successfully parsed as EC key with JDK KeyFactory: ${publicKey.algorithm}" }
                return publicKey
            } catch (e: Exception) {
                logger.info { "Failed to parse with JDK EC KeyFactory: ${e.message}" }
                
                // Try RSA as fallback
                try {
                    val keyFactory = KeyFactory.getInstance("RSA")
                    val keySpec = X509EncodedKeySpec(decoded)
                    val publicKey = keyFactory.generatePublic(keySpec)
                    logger.info { "Successfully parsed as RSA key with JDK KeyFactory: ${publicKey.algorithm}" }
                    return publicKey
                } catch (e2: Exception) {
                    logger.info { "Failed to parse with JDK RSA KeyFactory: ${e2.message}" }
                }
                
                // Try with BouncyCastle provider as last resort
                try {
                    val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider())
                    val keySpec = X509EncodedKeySpec(decoded)
                    val publicKey = keyFactory.generatePublic(keySpec)
                    logger.info { "Successfully parsed as EC key with BC provider: ${publicKey.algorithm}" }
                    return publicKey
                } catch (e3: Exception) {
                    logger.error(e3) { "All parsing attempts failed" }
                }
            }
        } else {
            logger.error { "Certificate doesn't contain BEGIN PUBLIC KEY marker" }
        }
        
        return null
    } catch (e: Exception) {
        logger.error(e) { "Error parsing certificate: ${e.message}" }
        return null
    }
}
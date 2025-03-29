package com.zenobiapay.bank.util

import com.zenobiapay.api.generated.models.CertificateType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CertificateValidatorTest {
    @Test
    fun `test certificate validator on valid certificate`() {
        assertTrue(
            isValidCertificate(
                "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUJoRENDQVNtZ0F3SUJBZ0lVYTN6SzRWTVl0V1kyWlpOWVA5cHlEbUxDMVI0d0NnWUlLb1pJemowRUF3SXcKRmpFVU1CSUdBMVVFQXd3TFpYaGhiWEJzWlM1amIyMHdJQmNOTWpVd016STVNREF6TlRNeVdoZ1BNakk1T1RBeApNVEl3TURNMU16SmFNQll4RkRBU0JnTlZCQU1NQzJWNFlXMXdiR1V1WTI5dE1Ga3dFd1lIS29aSXpqMENBUVlJCktvWkl6ajBEQVFjRFFnQUVEcW9oenl3NENaTjVyOUhKTXVkQlRuRDJ3dXdNazc3Y1JHeHdGTW56OWZQN25ZQmYKMXhTTkhBWk9WU01scVBWaHh1ZTNoN2xwZjZ5V3ltQkFmS1BoTHFOVE1GRXdIUVlEVlIwT0JCWUVGRWhGMS8vTQpqQnREVHhjdTZSY1kxSU9XTE5uRk1COEdBMVVkSXdRWU1CYUFGRWhGMS8vTWpCdERUeGN1NlJjWTFJT1dMTm5GCk1BOEdBMVVkRXdFQi93UUZNQU1CQWY4d0NnWUlLb1pJemowRUF3SURTUUF3UmdJaEFJc0ttaEZta0NTOG14cFEKSmJMYXpTWm01SmN6M3FBdC90SGs5TFNNMHF1eEFpRUFsKy9lb3haSnU0OG03RVdzblBOdzF5S201WWYwSHZRTQpKV1l2bTRiSzhwVT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=",
                CertificateType.EC
            )
        )
    }

    @Test
    fun `test invalid certificate`() {
        assertFalse(isValidCertificate("bad", CertificateType.EC))
    }
}
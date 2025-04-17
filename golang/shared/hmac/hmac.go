package hmac

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
)

func EncodeHmac(payload, secret string) string {
	key := []byte(secret)
	h := hmac.New(sha256.New, key)
	h.Write([]byte(payload))
	return base64.RawURLEncoding.EncodeToString(h.Sum(nil))
}

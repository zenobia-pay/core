package main

import (
	"context"
	"zenobia/shared/ddb"
	"zenobia/shared/hmac"
	"zenobia/shared/secrets"
)

func isValidRefreshToken(ctx context.Context, sub, refreshToken string) bool {
	hashedRefreshToken := hmac.EncodeHmac(refreshToken, secrets.GetRefreshTokenHashingSecret(ctx))
	isValid, err := ddb.GetHashedRefreshTokenFromCredentialsTable(ctx, sub, hashedRefreshToken)
	if err != nil {
		println("Error validating refresh token:", err.Error())
		return false
	}
	return isValid
}

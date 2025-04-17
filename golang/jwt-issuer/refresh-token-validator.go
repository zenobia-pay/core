package main

import (
	"context"
	"zenobia/shared/ddb"
	"zenobia/shared/hmac"
	"zenobia/shared/secrets"
)

func isValidRefreshToken(ctx context.Context, sub, refreshToken string) bool {
	if expectedRefreshToken, err := ddb.GetHashedRefreshTokenFromCredentialsTable(ctx, sub); err == nil {
		actualRefreshToken := hmac.EncodeHmac(refreshToken, secrets.GetRefreshTokenHashingSecret(ctx))
		// fmt.Printf("Got encoded refresh token %s\n", a)
		return actualRefreshToken == expectedRefreshToken
	}
	println("Failed to get table item for sub")
	return false
}

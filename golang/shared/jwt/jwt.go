package jwt

import (
	"context"
	"errors"
	"fmt"
	"log"
	"time"
	"zenobia/shared/secrets"

	"github.com/golang-jwt/jwt/v4"
)

type CustomerClaims struct {
	Role string
	jwt.RegisteredClaims
}

var issuer string = "https://api.zenobiapay.com"
var ExpiryTimeSeconds int = 900

func IssueJWT(ctx context.Context, sub string) (string, error) {
	hmacSecret := secrets.GetJwtTokenHashingSecret(ctx)
	claims := CustomerClaims{
		Role: "CUSTOMER",
		RegisteredClaims: jwt.RegisteredClaims{
			Subject:   sub,
			Issuer:    issuer,
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Duration(ExpiryTimeSeconds) * time.Second)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(hmacSecret))
}

func ValidateCustomerJwt(ctx context.Context, tokenString string) (*CustomerClaims, error) {
	hmacSecret := secrets.GetJwtTokenHashingSecret(ctx)
	println("fetched hmac secret")

	println("Starting parse with claims")
	token, err := jwt.ParseWithClaims(tokenString, &CustomerClaims{}, func(token *jwt.Token) (interface{}, error) {
		// Ensure token is signed with HMAC
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return []byte(hmacSecret), nil
	})
	if err != nil {
		log.Println("Token invalid:", err)
		return nil, err
	}
	if claims, ok := token.Claims.(*CustomerClaims); ok && token.Valid {
		fmt.Println("Token is valid. Claims:", claims)
		if claims.Issuer != issuer {
			fmt.Printf("Unknown issuer %s", claims.Issuer)
			return nil, errors.New("unknown issuer")
		}
		return claims, nil
	} else {
		log.Println("Invalid token")
		return nil, fmt.Errorf("invalid token")
	}
}

package main

import (
	"context"
	"errors"
	"net/url"
	"os"
	"time"

	"github.com/auth0/go-jwt-middleware/v2/jwks"
	"github.com/auth0/go-jwt-middleware/v2/validator"
)

// CustomClaims contains custom data we want from the token.
type CustomClaims struct {
	Scope string `json:"scope"`
	Azp   string `json:"azp"`
}

// Validates that azp is auth0 app client
func (c CustomClaims) Validate(ctx context.Context) error {
	auth0ClientId, found := os.LookupEnv("AUTH0_CLIENT_ID")
	if !found {
		println("Did not find auth0 client env var")
		return errors.New("did not find AUTH0_CLIENT_ID")
	}
	if c.Azp != auth0ClientId {
		return errors.New("auth 0 client id did not match azp")
	}
	return nil
}

var basicJwtValidator *validator.Validator
var auth0ActionJwtValidator *validator.Validator
var provider *jwks.CachingProvider

func init() {
	domain := os.Getenv("AUTH_DOMAIN")
	audience := os.Getenv("AUDIENCE")

	if domain == "" {
		panic("Did not retrieve env var AUTH_DOMAIN")
	}
	if audience == "" {
		panic("Did not retrieve env var AUDIENCE")
	}

	issuerURL, err := url.Parse("https://" + domain + "/")
	if err != nil {
		panic("Failed to parse the issuer url " + err.Error())
	}

	provider = jwks.NewCachingProvider(issuerURL, 5*time.Minute)
	basicJwtValidator, err = validator.New(
		provider.KeyFunc,
		validator.RS256,
		issuerURL.String(),
		[]string{audience},
		validator.WithAllowedClockSkew(time.Minute),
	)

	if err != nil {
		panic("Failed to set up the jwt validator " + err.Error())
	}

	auth0ActionJwtValidator, err = validator.New(
		provider.KeyFunc,
		validator.RS256,
		issuerURL.String(),
		[]string{audience},
		validator.WithCustomClaims(
			func() validator.CustomClaims {
				return &CustomClaims{}
			},
		),
		validator.WithAllowedClockSkew(time.Minute),
	)
	if err != nil {
		panic("Failed to set up the jwt validator " + err.Error())
	}
}

// EnsureValidToken is a middleware that will check the validity of our JWT.
func EnsureValidToken(ctx context.Context, token string) bool {
	_, err := basicJwtValidator.ValidateToken(ctx, token)
	return err == nil
}

func EnsureValidAuth0ActionToken(ctx context.Context, token string) bool {
	_, err := auth0ActionJwtValidator.ValidateToken(ctx, token)
	return err == nil
}

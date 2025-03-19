package main

import (
	"context"
	"net/url"
	"os"
	"time"

	"github.com/auth0/go-jwt-middleware/v2/jwks"
	"github.com/auth0/go-jwt-middleware/v2/validator"
)

// CustomClaims contains custom data we want from the token.
type CustomClaims struct {
	Scope string `json:"scope"`
}

// Validate does nothing for this example, but we need
// it to satisfy validator.CustomClaims interface.
func (c CustomClaims) Validate(ctx context.Context) error {
	return nil
}

var jwtValidator *validator.Validator
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
	jwtValidator, err = validator.New(
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
	_, err := jwtValidator.ValidateToken(ctx, token)
	return err == nil
}

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

// MachineCustomClaims contains custom data we want from the token.
type MachineCustomClaims struct {
	Scope string `json:"scope"`
	Azp   string `json:"azp"`
	Role  string `json:"role"`
}

// Validates that azp is auth0 app client
func (c MachineCustomClaims) Validate(ctx context.Context) error {
	return nil
}

// UserCustomClaims contains custom data we want from the token.
type UserCustomClaims struct {
	Role   *string `json:"role"`
	Email  *string `json:"email"`
	M2MSub *string `json:"m2mSub"`
}

// Validates that azp is auth0 app client
func (c UserCustomClaims) Validate(ctx context.Context) error {
	return nil
}

var basicJwtValidator *validator.Validator
var auth0ActionJwtValidator *validator.Validator
var provider *jwks.CachingProvider

func init() {
	domain := os.Getenv("AUTH_DOMAIN")
	issuer := "https://" + domain + "/"
	audience := os.Getenv("AUDIENCE")

	if domain == "" {
		panic("Did not retrieve env var AUTH_DOMAIN")
	}
	if audience == "" {
		panic("Did not retrieve env var AUDIENCE")
	}

	println("Got domain " + domain + ", issuer " + issuer + ", audience " + audience)

	zenobiaIssuerUrl, err := url.Parse(issuer)
	if err != nil {
		panic("Failed to parse the zenobia issuer url " + err.Error())
	}

	provider = jwks.NewCachingProvider(zenobiaIssuerUrl, 5*time.Minute)
	basicJwtValidator, err = validator.New(
		provider.KeyFunc,
		validator.RS256,
		zenobiaIssuerUrl.String(),
		[]string{audience},
		validator.WithCustomClaims(
			func() validator.CustomClaims {
				return &UserCustomClaims{}
			},
		),
		validator.WithAllowedClockSkew(time.Minute),
	)

	if err != nil {
		panic("Failed to set up the jwt validator " + err.Error())
	}
	if err != nil {
		panic("Failed to set up the jwt validator " + err.Error())
	}
}

// GetValidatedUserClaims is a middleware that will check the validity of our JWT.
func GetValidatedUserClaims(ctx context.Context, token string) (*validator.ValidatedClaims, error) {
	claims, err := basicJwtValidator.ValidateToken(ctx, token)
	if err != nil {
		println("Validation threw err", err.Error())
		return nil, err
	}
	return getCastClaims(claims)
}

func getCastClaims(claims interface{}) (*validator.ValidatedClaims, error) {
	if castClaims, ok := claims.(*validator.ValidatedClaims); ok {
		return castClaims, nil
	} else {
		println("Failed to cast claim")
		return nil, errors.New("could not cast claim")
	}
}

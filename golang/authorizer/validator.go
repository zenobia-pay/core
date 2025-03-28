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

// MachineCustomClaims contains custom data we want from the token.
type UserCustomClaims struct {
	Role  string `json:"role"`
	Email string `json:"email"`
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
	issuer := os.Getenv("ZENOBIA_ISSUER")
	audience := os.Getenv("AUDIENCE")

	if domain == "" {
		panic("Did not retrieve env var AUTH_DOMAIN")
	}
	if issuer == "" {
		panic("Did not retrieve env var ZENOBIA_ISSUER")
	}
	if audience == "" {
		panic("Did not retrieve env var AUDIENCE")
	}

	zenobiaIssuerUrl, err := url.Parse(issuer)
	if err != nil {
		panic("Failed to parse the zenobia issuer url " + err.Error())
	}

	auth0IssuerUrl, err := url.Parse("https://" + domain + "/")
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

	auth0ActionJwtValidator, err = validator.New(
		provider.KeyFunc,
		validator.RS256,
		auth0IssuerUrl.String(),
		[]string{audience},
		validator.WithCustomClaims(
			func() validator.CustomClaims {
				return &MachineCustomClaims{}
			},
		),
		validator.WithAllowedClockSkew(time.Minute),
	)
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

func GetValidatedAuth0ActionClaims(ctx context.Context, token string) (*validator.ValidatedClaims, error) {
	claims, err := auth0ActionJwtValidator.ValidateToken(ctx, token)
	if err != nil {
		println("Auth0 action validation threw err", err.Error())
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

package main

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/auth0/go-jwt-middleware/v2/validator"
	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func handler(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got path " + event.Path)
	token := extractToken(event.Headers["Authorization"])
	if event.Path == "/register-user" {
		println("Validating auth0 token")
		claims, err := GetValidatedAuth0ActionClaims(ctx, token)
		isValid := err == nil
		println(fmt.Sprintf("Got isValidAuth0ActionToken: %t", isValid))
		return generatePolicyResponse(isValid, claims, event.MethodArn), nil
	} else {
		println("Validating auth0 user")
		claims, err := GetValidatedUserClaims(ctx, token)
		isValid := err == nil
		println(fmt.Sprintf("Got isValidApiToken: %t", isValid))
		return generatePolicyResponse(isValid, claims, event.MethodArn), nil
	}
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
}

func generatePolicyResponse(isValid bool, claims *validator.ValidatedClaims, methodArn string) events.APIGatewayCustomAuthorizerResponse {
	if isValid && claims != nil {
		return generatePolicy("user", "Allow", wildcardArn(methodArn), claims)
	} else {
		return generatePolicy("user", "Deny", "*", nil)
	}
}

func generatePolicy(principalID, effect, resource string, claims *validator.ValidatedClaims) events.APIGatewayCustomAuthorizerResponse {
	var context map[string]interface{} = nil
	if claims != nil {
		var role *string = nil
		if castCustomClaims, ok := claims.CustomClaims.(CustomClaims); ok {
			role = &castCustomClaims.Role
			println("Got role: " + *role)
		}
		context = map[string]interface{}{
			"sub":  claims.RegisteredClaims.Subject,
			"role": role,
		}
	}

	return events.APIGatewayCustomAuthorizerResponse{
		PrincipalID: principalID,
		PolicyDocument: events.APIGatewayCustomAuthorizerPolicy{
			Version: "2012-10-17",
			Statement: []events.IAMPolicyStatement{
				{
					Action:   []string{"execute-api:Invoke"},
					Effect:   effect,
					Resource: []string{resource},
				},
			},
		},
		Context: context,
	}
}

func wildcardArn(methodArn string) string {
	// TODO: blocker! use restricted wildcard
	// Example: arn:aws:execute-api:us-east-1:123456789012:abc123/prod/GET/resource
	println("Got original method arn" + methodArn)
	parts := strings.Split(methodArn, "/")

	if len(parts) < 4 {
		return methodArn
	}
	// Build: arn:aws:execute-api:{region}:{account}:{apiId}/{stage}/*/*
	wildcardArn := fmt.Sprintf("%s/*/*", strings.Join(parts[:2], "/"))
	println("Got wildcard arn " + wildcardArn)
	return wildcardArn
}

func main() {
	if len(os.Args) > 1 {
		println("Got arguments with invocation. Running in local mode. Should not run this in prod!")
		_, err := GetValidatedUserClaims(context.Background(), os.Args[1])
		println("Got value", err != nil)
	}
	lambda.Start(handler)
}

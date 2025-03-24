package main

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func handler(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got path " + event.Path)
	token := extractToken(event.Headers["Authorization"])
	if event.Path == "/register-user" {
		println("Validating auth0 token")
		isValid := EnsureValidAuth0ActionToken(ctx, token)
		println(fmt.Sprintf("Got isValidAuth0ActionToken: %t", isValid))
		return generatePolicyResponse(isValid, event.MethodArn), nil
	} else {
		println("Validating auth0 user")
		isValid := EnsureValidToken(ctx, token)
		println(fmt.Sprintf("Got isValidApiToken: %t", isValid))
		return generatePolicyResponse(isValid, event.MethodArn), nil
	}
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
}

func generatePolicyResponse(isValid bool, methodArn string) events.APIGatewayCustomAuthorizerResponse {
	if isValid {
		return generatePolicy("user", "Allow", methodArn)
	} else {
		return generatePolicy("user", "Deny", "*")
	}
}

func generatePolicy(principalID, effect, resource string) events.APIGatewayCustomAuthorizerResponse {
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
	}
}

func main() {
	if len(os.Args) > 1 {
		println("Got arguments with invocation. Running in local mode. Should not run this in prod!")
		isValid := EnsureValidAuth0ActionToken(context.Background(), os.Args[1])
		println("Got value", isValid)
	}
	lambda.Start(handler)
}

package main

import (
	"context"
	"os"
	"strings"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func handler(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	token := extractToken(event.Headers["Authorization"])
	isValid := EnsureValidToken(ctx, token)

	if isValid {
		return generatePolicy("user", "Allow", event.MethodArn), nil
	} else {
		return generatePolicy("user", "Deny", "*"), nil
	}
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
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
		token := os.Args[1]
		println("Got args, checking token " + token)
		isValid := EnsureValidToken(context.Background(), token)
		if isValid {
			println("Valid token!")
		} else {
			println("Invalid token!")
		}
	} else {
		lambda.Start(handler)
	}
}

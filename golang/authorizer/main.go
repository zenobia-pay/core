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
	println("Validating auth0 user")
	claims, err := GetValidatedUserClaims(ctx, token)
	isValid := err == nil
	println(fmt.Sprintf("Got isValidApiToken: %t", isValid))
	return generatePolicyResponse(isValid, getUserContext(claims), event.MethodArn), nil
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
}

func getUserContext(claims *validator.ValidatedClaims) map[string]interface{} {
	if userCustomClaims, ok := claims.CustomClaims.(*UserCustomClaims); ok {
		context := map[string]interface{}{
			"sub":   claims.RegisteredClaims.Subject,
			"email": userCustomClaims.Email,
			"role":  userCustomClaims.Role,
		}
		print("Got context: ")
		fmt.Println(context)
		return context
	}
	print("Could not cast claims to user custom claims")
	return map[string]interface{}{}
}

func generatePolicyResponse(isValid bool, context map[string]interface{}, methodArn string) events.APIGatewayCustomAuthorizerResponse {
	if isValid {
		return events.APIGatewayCustomAuthorizerResponse{
			PrincipalID: "user",
			PolicyDocument: events.APIGatewayCustomAuthorizerPolicy{
				Version: "2012-10-17",
				Statement: []events.IAMPolicyStatement{
					{
						Action:   []string{"execute-api:Invoke"},
						Effect:   "Allow",
						Resource: []string{wildcardArn(methodArn)},
					},
				},
			},
			Context: context,
		}
	} else {
		return events.APIGatewayCustomAuthorizerResponse{
			PrincipalID: "user",
			PolicyDocument: events.APIGatewayCustomAuthorizerPolicy{
				Version: "2012-10-17",
				Statement: []events.IAMPolicyStatement{
					{
						Action:   []string{"execute-api:Invoke"},
						Effect:   "Deny",
						Resource: []string{"*"},
					},
				},
			},
		}
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
		claims, err := GetValidatedUserClaims(context.Background(), os.Args[1])
		fmt.Printf("Got claims: %+v\n", claims)
		fmt.Printf("Context: %+v\n", getUserContext(claims))

		println("Got value", err != nil)
	}
	lambda.Start(handler)
}

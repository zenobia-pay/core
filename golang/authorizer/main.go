package main

import (
	"context"
	"fmt"
	"os"
	"strings"
	"zenobia/shared/jwt"
	"zenobia/shared/secrets"

	"github.com/auth0/go-jwt-middleware/v2/validator"
	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

func handler(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got path " + event.Path)

	var hasAuthorizationHeader = false
	if authorization, ok := event.Headers["Authorization"]; ok {
		hasAuthorizationHeader = authorization != "NONE" // Explicitly set by app if no auth is provided
	}
	if isValidPath(event.Path, validOrumRoutes) {
		return handleOrumWebhookEndpoint(ctx, event)
	} else if !hasAuthorizationHeader && isValidPath(event.Path, validUnauthenticatedRoutes) {
		return handleUnprotectedEndpoint(ctx, event)
	} else if isValidPath(event.Path, validCustomerRoutes) || isValidPath(event.Path, validMerchantRoutes) {
		return handleProtectedEndpoint(ctx, event)
	} else {
		println("Could not find endpoint. Returning blanket deny.")
		return generateDenyPolicyResponse(), nil
	}
}

func handleOrumWebhookEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got webhook. Validating ip address")
	orum_ip_addresses, ok := os.LookupEnv("VALID_ORUM_IP_ADDRESSES")
	print("valid ip addresses: ")
	fmt.Println(orum_ip_addresses)

	if !ok {
		panic("failed to fetch valid orum ip addresses")
	}

	allowedIps := strings.Split(orum_ip_addresses, ",")
	ip := event.RequestContext.Identity.SourceIP
	println("Got request ip: " + ip)
	for _, allowedIp := range allowedIps {
		if ip == allowedIp {
			println("Matched ip address, allowing")
			return generatePolicy("user", "Allow", []string{event.MethodArn}, map[string]interface{}{}), nil
		}
	}
	println("IP address not recognized, denying")
	return generatePolicy("user", "Deny", []string{"*"}, map[string]interface{}{}), nil
}

func handleUnprotectedEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Validating unauthenticated user. Returning allow")
	paths, err := generateOperationArns(event.MethodArn, validUnauthenticatedRoutes)
	if err != nil {
		panic("Could not generate unauthenticated arn paths")
	}
	return generatePolicyResponse(true, getUserContext(nil), paths), nil
}

func handleProtectedEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	token := extractToken(event.Headers["Authorization"])
	if isValidPath(event.Path, validCustomerRoutes) {
		println("Attempting to validate token as customer")
		context, isValid := handleCustomerJwtTokens(ctx, token)
		if isValid {
			paths, err := generateOperationArns(event.MethodArn, validCustomerRoutes)
			if err != nil {
				panic("Could not generate authenticated customer arn paths")
			}
			return generatePolicyResponse(isValid, context, paths), nil
		}
	}
	if isValidPath(event.Path, validMerchantRoutes) {
		println("Attempting to validate token as merchant/m2m user")
		context, isValid := handleAuth0Tokens(ctx, token)
		if isValid {
			paths, err := generateOperationArns(event.MethodArn, validMerchantRoutes)
			if err != nil {
				panic("Could not generate authenticated merchant arn paths")
			}
			return generatePolicyResponse(isValid, context, paths), nil
		}

	}
	return generatePolicy("user", "Deny", []string{"*"}, map[string]interface{}{}), nil
}

func handleAuth0Tokens(ctx context.Context, token string) (map[string]interface{}, bool) {
	claims, err := GetValidatedUserClaims(ctx, token)
	isValid := err == nil
	println(fmt.Sprintf("Got isValidApiToken: %t", isValid))
	return getUserContext(claims), isValid
}

func handleCustomerJwtTokens(ctx context.Context, token string) (map[string]interface{}, bool) {
	claims, err := jwt.ValidateCustomerJwt(ctx, token)
	isValid := err == nil
	fmt.Printf("Got isValidCustomerJwtToken: %t", isValid)
	return getCustomerContext(*claims), isValid
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
}

func getCustomerContext(claims jwt.CustomerClaims) map[string]interface{} {
	context := map[string]interface{}{
		"sub":  claims.Subject,
		"role": claims.Role,
	}
	fmt.Printf("Got context: %+v\n", context)
	return context
}

func getUserContext(claims *validator.ValidatedClaims) map[string]interface{} {
	if claims == nil {
		return nil
	}

	if userCustomClaims, ok := claims.CustomClaims.(*UserCustomClaims); ok {
		context := map[string]interface{}{
			"sub":    claims.RegisteredClaims.Subject,
			"email":  userCustomClaims.Email,
			"role":   userCustomClaims.Role,
			"m2mSub": userCustomClaims.M2MSub,
		}
		print("Got context: ")
		fmt.Println(context)
		return context
	}
	println("Could not cast user custom claims")
	return nil
}

func generatePolicyResponse(isValid bool, context map[string]interface{}, arns []string) events.APIGatewayCustomAuthorizerResponse {
	if isValid {
		return generatePolicy("user", "Allow", arns, context)
	} else {
		return generatePolicy("user", "Deny", []string{"*"}, context)
	}
}

func generateDenyPolicyResponse() events.APIGatewayCustomAuthorizerResponse {
	return generatePolicy("user", "Deny", []string{"*"}, nil)
}

func generatePolicy(principalId, effect string, resource []string, context map[string]interface{}) events.APIGatewayCustomAuthorizerResponse {
	authResponse := events.APIGatewayCustomAuthorizerResponse{PrincipalID: principalId}

	if effect != "" && len(resource) > 0 {
		authResponse.PolicyDocument = events.APIGatewayCustomAuthorizerPolicy{
			Version: "2012-10-17",
			Statement: []events.IAMPolicyStatement{
				{
					Action:   []string{"execute-api:Invoke"},
					Effect:   effect,
					Resource: resource,
				},
			},
		}
	}

	if context != nil {
		authResponse.Context = context
	}
	return authResponse
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
	secrets.InitSecretsClient(context.Background())
	lambda.Start(handler)
}

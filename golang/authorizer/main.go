package main

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"zenobia/shared/cloudwatch"
	"zenobia/shared/jwt"
	"zenobia/shared/secrets"

	"github.com/auth0/go-jwt-middleware/v2/validator"
	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

const namespace = "Authorizer"

func handler(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	fmt.Printf("Got requestId %s, path %s\n", event.RequestContext.RequestID, event.Path)
	b, err := json.MarshalIndent(event, "", "  ")
	if err != nil {
		panic("error is not nil")
	}
	// TODO: remove
	fmt.Print("FULL EVENT")
	fmt.Println(string(b))

	var hasAuthorizationHeader = false
	if authorization, ok := event.Headers["Authorization"]; ok {
		hasAuthorizationHeader = authorization != "NONE" // Explicitly set by app if no auth is provided
	}
	if isValidPath(event.Path, validOrumRoutes) {
		return handleOrumWebhookEndpoint(ctx, event)
	} else if isValidPath(event.Path, validPlaidRoutes) {
		return handlePlaidWebhookEndpoint(ctx, event)
	} else if !hasAuthorizationHeader && isValidPath(event.Path, validUnauthenticatedRoutes) {
		return handleUnprotectedEndpoint(ctx, event)
	} else if isValidPath(event.Path, validCustomerRoutes) || isValidPath(event.Path, validMerchantRoutes) {
		return handleProtectedEndpoint(ctx, event)
	} else {
		println("Could not find endpoint. Returning blanket deny.")
		cloudwatch.PutMetric(context.Background(), "UnknownEndpoint", 1.0, namespace)
		return generateDenyPolicyResponse(), nil
	}
}

func handleOrumWebhookEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got orum webhook. Validating ip address")
	orum_ip_addresses, ok := os.LookupEnv("VALID_ORUM_IP_ADDRESSES")
	print("valid orum ip addresses: ")
	fmt.Println(orum_ip_addresses)

	if !ok {
		panic("failed to fetch valid orum ip addresses")
	}
	return handleIpRestrictedEndpoint(ctx, event.RequestContext.Identity.SourceIP, orum_ip_addresses, event.MethodArn)
}

func handlePlaidWebhookEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Got plaid webhook. Validating ip address")
	plaid_ip_addresses, ok := os.LookupEnv("VALID_PLAID_IP_ADDRESSES")
	print("valid orum ip addresses: ")
	fmt.Println(plaid_ip_addresses)

	if !ok {
		panic("failed to fetch valid plaid ip addresses")
	}
	return handleIpRestrictedEndpoint(ctx, event.RequestContext.Identity.SourceIP, plaid_ip_addresses, event.MethodArn)
}

func handleIpRestrictedEndpoint(ctx context.Context, sourceIp, validIpString, methodArn string) (events.APIGatewayCustomAuthorizerResponse, error) {
	allowedIps := strings.Split(validIpString, ",")
	println("Got request ip: " + sourceIp)
	for _, allowedIp := range allowedIps {
		if sourceIp == allowedIp {
			println("Matched ip address, allowing")
			putSuccessMetric(true)
			return generatePolicy("user", "Allow", []string{methodArn}, map[string]interface{}{}), nil
		}
	}
	println("IP address not recognized, denying")
	putSuccessMetric(false)
	return generatePolicy("user", "Deny", []string{"*"}, map[string]interface{}{}), nil
}

func handleUnprotectedEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	println("Validating unauthenticated user. Returning allow")
	paths, err := generateOperationArns(event.MethodArn, validUnauthenticatedRoutes)
	if err != nil {
		panic("Could not generate unauthenticated arn paths")
	}
	putSuccessMetric(true)
	return generatePolicyResponse(true, getUserContext(nil), paths), nil
}

func handleProtectedEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	authorizationHeader := event.Headers["Authorization"]
	if authorizationHeader == "" {
		authorizationHeader = event.Headers["authorization"]
	}
	token := extractToken(authorizationHeader)
	if isValidPath(event.Path, validCustomerRoutes) {
		println("Attempting to validate token as customer")
		context, isValid := handleCustomerJwtTokens(ctx, token)
		if isValid {
			paths, err := generateOperationArns(event.MethodArn, validCustomerRoutes)
			if err != nil {
				panic("Could not generate authenticated customer arn paths")
			}
			putSuccessMetric(isValid)
			return generatePolicyResponse(isValid, context, paths), nil
		}
	}
	if isValidPath(event.Path, validMerchantRoutes) {
		println("Attempting to validate token as merchant/m2m user")
		// TODO: remove
		fmt.Printf("Got jwt %s\n", token)
		context, isValid := handleAuth0Tokens(ctx, token)
		if isValid {
			paths, err := generateOperationArns(event.MethodArn, validMerchantRoutes)
			if err != nil {
				panic("Could not generate authenticated merchant arn paths")
			}
			putSuccessMetric(isValid)
			return generatePolicyResponse(isValid, context, paths), nil
		}

	}
	putSuccessMetric(false)
	return generatePolicy("user", "Deny", []string{"*"}, map[string]interface{}{}), nil
}

func handleAuth0Tokens(ctx context.Context, token string) (map[string]interface{}, bool) {
	claims, err := GetValidatedUserClaims(ctx, token)
	isValid := err == nil
	println(fmt.Sprintf("Got isValidApiToken: %t\n", isValid))
	return getUserContext(claims), isValid
}

func handleCustomerJwtTokens(ctx context.Context, token string) (map[string]interface{}, bool) {
	claims, err := jwt.ValidateCustomerJwt(ctx, token)
	isValid := err == nil
	fmt.Printf("Got isValidCustomerJwtToken: %t\n", isValid)
	return getCustomerContext(claims), isValid
}

func extractToken(authHeader string) string {
	// TODO: remove
	fmt.Printf("Got auth header %s", authHeader)
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
}

func getCustomerContext(claims *jwt.CustomerClaims) map[string]interface{} {
	if claims == nil {
		return nil
	}
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

func putSuccessMetric(success bool) {
	var value float64
	if success {
		value = 1.0
	} else {
		value = 0.0
	}
	cloudwatch.PutMetric(context.Background(), "Success", value, namespace)
}

func main() {
	secrets.InitSecretsClient(context.Background())
	cloudwatch.InitCloudWatch(context.Background())
	lambda.Start(handler)
}

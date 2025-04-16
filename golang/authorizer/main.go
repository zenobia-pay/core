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

	var hasAuthorizationHeader = false
	if authorization, ok := event.Headers["Authorization"]; ok {
		hasAuthorizationHeader = authorization != "NONE" // Explicitly set by app if no auth is provided
	}
	if isValidPath(event.Path, validOrumRoutes) {
		return handleOrumWebhookEndpoint(ctx, event)
	} else if !hasAuthorizationHeader && isValidPath(event.Path, validUnauthenticatedRoutes) {
		return handleUnprotectedEndpoint(ctx, event)
	} else {
		return handleProtectedEndpoint(ctx, event)
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
	paths, err := generateUnauthenticatedArnPaths(event.MethodArn)
	if err != nil {
		return generatePolicyResponse(false, getUserContext(nil), []string{}), nil
	}
	return generatePolicyResponse(true, getUserContext(nil), paths), nil
}

func handleProtectedEndpoint(ctx context.Context, event events.APIGatewayCustomAuthorizerRequestTypeRequest) (events.APIGatewayCustomAuthorizerResponse, error) {
	token := extractToken(event.Headers["Authorization"])
	println("Validating auth0 user")
	claims, err := GetValidatedUserClaims(ctx, token)
	isValid := err == nil
	println(fmt.Sprintf("Got isValidApiToken: %t", isValid))
	return generatePolicyResponse(isValid, getUserContext(claims), []string{wildcardArn(event.MethodArn)}), nil
}

func extractToken(authHeader string) string {
	parts := strings.Split(authHeader, " ")
	if len(parts) == 2 && parts[0] == "Bearer" {
		return parts[1]
	}
	return ""
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

func generateUnauthenticatedArnPaths(methodArn string) ([]string, error) {
	var arns []string
	for _, v := range validUnauthenticatedRoutes {
		arn, err := getOperationArn(methodArn, v)
		if err != nil {
			return nil, err
		}
		arns = append(arns, *arn)
	}
	return arns, nil
}

func getOperationArn(methodArn string, route Route) (*string, error) {
	parts := strings.Split(methodArn, "/")

	if len(parts) < 4 {
		return nil, fmt.Errorf("Invalid methodArn format %s", methodArn)
	}

	arn := fmt.Sprintf("%s/%s/%s", strings.Join(parts[:2], "/"), route.Method, route.Path)
	println("Got generated arn:" + arn)
	return &arn, nil
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

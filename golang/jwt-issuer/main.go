package main

import (
	"context"
	"encoding/json"
	"fmt"
	"zenobia/shared/cloudwatch"
	"zenobia/shared/ddb"
	"zenobia/shared/jwt"
	"zenobia/shared/secrets"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
)

const namespace = "JwtIssuer"

func main() {
	ddb.InitDDB(context.Background())
	cloudwatch.InitCloudWatch(context.Background())
	secrets.InitSecretsClient(context.Background())
	lambda.Start(handler)
}

func handler(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	println("Recieved request to fetch jwt token")
	if request.Path != "/issue-jwt" {
		fmt.Printf("Invalid path %s", request.RequestContext.Path)
		return generateInvalidRequestResponse(), nil
	}
	var requestMap map[string]string
	if err := json.Unmarshal([]byte(request.Body), &requestMap); err != nil {
		println("Could not deserialize body")
		return generateInvalidRequestResponse(), nil
	}

	sub, ok := requestMap["sub"]
	if !ok {
		println("Did not find sub")
		return generateInvalidRequestResponse(), nil
	}
	fmt.Printf("Attempting to authorize sub %s", sub)
	refreshToken, ok := requestMap["refreshToken"]
	if !ok {
		println("Did not find refreshToken")
		return generateInvalidRequestResponse(), nil
	}

	if !isValidRefreshToken(ctx, sub, refreshToken) {
		fmt.Printf("Token was invalid for sub %s", sub)
		return generateUnauthorizedResponse(), nil
	}

	issuedJwt, error := jwt.IssueJWT(ctx, sub)
	if error != nil {
		cloudwatch.PutMetric(context.Background(), "JwtIssueFailure", 1.0, namespace)
		panic("Failed to issue jwt")
	}
	body := map[string]any{
		"jwt":       issuedJwt,
		"expiresIn": jwt.ExpiryTimeSeconds,
	}
	marshalledBody, error := json.Marshal(body)
	if error != nil {
		panic("Failed to marshall body")
	}
	cloudwatch.PutMetric(context.Background(), "Success", 1.0, namespace)
	return events.APIGatewayProxyResponse{
		StatusCode: 200,
		Body:       string(marshalledBody),
	}, nil
}

func generateInvalidRequestResponse() events.APIGatewayProxyResponse {
	cloudwatch.PutMetric(context.Background(), "InvalidRequest", 1.0, namespace)
	return events.APIGatewayProxyResponse{
		StatusCode: 400,
		Body:       "{}",
	}
}

func generateUnauthorizedResponse() events.APIGatewayProxyResponse {
	cloudwatch.PutMetric(context.Background(), "Unauthorized", 1.0, namespace)
	return events.APIGatewayProxyResponse{
		StatusCode: 403,
		Body:       "{}",
	}
}

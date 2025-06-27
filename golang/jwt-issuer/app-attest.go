package main

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"zenobia/shared/cloudwatch"
	"zenobia/shared/ddb"

	"github.com/aws/aws-lambda-go/events"
)

const (
	// TTL for challenge in seconds (5 minutes)
	challengeTTL = 300
	// Size of the random challenge in bytes
	challengeSize = 32
)

// ChallengeResponse is the response returned to the client
type ChallengeResponse struct {
	Challenge string `json:"challenge"`
	RequestID string `json:"requestId"`
}

// generateChallenge handles the /generate-challenge endpoint
func generateChallenge(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	println("Received request to generate app attest challenge")

	// Generate a random challenge
	challenge, err := generateRandomChallenge()
	if err != nil {
		cloudwatch.PutMetric(ctx, "ChallengeGenerationFailure", 1.0, namespace)
		fmt.Printf("Failed to generate challenge: %v", err)
		return events.APIGatewayProxyResponse{
			StatusCode: 500,
			Body:       "{}",
		}, nil
	}

	requestID := request.RequestContext.RequestID
	// Store the challenge in DynamoDB with TTL
	err = ddb.StoreChallenge(ctx, requestID, challenge, challengeTTL)
	if err != nil {
		cloudwatch.PutMetric(ctx, "ChallengeStorageFailure", 1.0, namespace)
		fmt.Printf("Failed to store challenge: %v", err)
		return events.APIGatewayProxyResponse{
			StatusCode: 500,
			Body:       "{}",
		}, nil
	}

	// Create the response
	response := ChallengeResponse{
		Challenge: challenge,
		RequestID: requestID,
	}

	// Marshal the response to JSON
	responseBody, err := json.Marshal(response)
	if err != nil {
		cloudwatch.PutMetric(ctx, "ResponseMarshalFailure", 1.0, namespace)
		fmt.Printf("Failed to marshal response: %v", err)
		return events.APIGatewayProxyResponse{
			StatusCode: 500,
			Body:       "{}",
		}, nil
	}

	cloudwatch.PutMetric(ctx, "ChallengeGenerationSuccess", 1.0, namespace)
	return events.APIGatewayProxyResponse{
		StatusCode: 200,
		Body:       string(responseBody),
	}, nil
}

// generateRandomChallenge generates a cryptographically secure random challenge
func generateRandomChallenge() (string, error) {
	bytes := make([]byte, challengeSize)
	_, err := rand.Read(bytes)
	if err != nil {
		return "", fmt.Errorf("failed to generate random bytes: %w", err)
	}

	// Encode the random bytes as base64
	return base64.StdEncoding.EncodeToString(bytes), nil
}

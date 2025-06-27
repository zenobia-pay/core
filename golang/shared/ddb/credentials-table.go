package ddb

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb/types"
	"github.com/aws/aws-sdk-go-v2/feature/dynamodb/attributevalue"
)

var ddbClient *dynamodb.Client
var credentialsTableName string

func GetHashedRefreshTokenFromCredentialsTable(ctx context.Context, sub, refreshToken string) (bool, error) {
	out, err := ddbClient.GetItem(ctx, &dynamodb.GetItemInput{
		TableName: aws.String(credentialsTableName),
		Key: map[string]types.AttributeValue{
			"pk": &types.AttributeValueMemberS{Value: sub},
			"sk": &types.AttributeValueMemberS{Value: refreshToken},
		},
	})

	if err != nil {
		return false, fmt.Errorf("unexpected ddb error: %w", err)
	}

	// If the item exists with the given pk and sk, the token is valid
	return out.Item != nil, nil
}

// storeChallenge stores the challenge in DynamoDB with TTL
func StoreChallenge(ctx context.Context, requestID, challenge string, challengeTTL int64) error {
	// Calculate TTL (current time + TTL in seconds)
	ttl := time.Now().Unix() + challengeTTL

	// Create the challenge item
	challengeItem := AppAttestChallenge{
		PK:        "APP_ATTEST_CHALLENGE",
		SK:        requestID,
		Challenge: challenge,
		TTL:       ttl,
	}

	// Convert the challenge item to DynamoDB attribute values
	item, err := attributevalue.MarshalMap(challengeItem)
	if err != nil {
		return fmt.Errorf("failed to marshal challenge item: %w", err)
	}

	// Store the challenge in DynamoDB
	_, err = ddbClient.PutItem(ctx, &dynamodb.PutItemInput{
		TableName: aws.String(credentialsTableName),
		Item:      item,
	})

	if err != nil {
		return fmt.Errorf("failed to store challenge in DynamoDB: %w", err)
	}
	return nil
}

// getStoredChallenge retrieves a challenge from DynamoDB by request ID
func GetStoredChallenge(ctx context.Context, requestID string) (string, error) {
	// Get the challenge from DynamoDB
	result, err := ddbClient.GetItem(ctx, &dynamodb.GetItemInput{
		TableName: aws.String(credentialsTableName),
		Key: map[string]types.AttributeValue{
			"pk": &types.AttributeValueMemberS{Value: "APP_ATTEST_CHALLENGE"},
			"sk": &types.AttributeValueMemberS{Value: requestID},
		},
	})

	if err != nil {
		return "", fmt.Errorf("failed to get challenge from DynamoDB: %w", err)
	}

	// Check if the challenge exists
	if result.Item == nil {
		return "", fmt.Errorf("challenge not found for request ID: %s", requestID)
	}

	// Convert the DynamoDB item to a challenge
	// var challenge AppAttestChallenge
	// if err := attributevalue.UnmarshalMap(result.Item, &challenge); err != nil {
	// 	return "", fmt.Errorf("failed to unmarshal challenge: %w", err)
	// }
	return "", nil

	// return challenge.Challenge, nil
}

func InitDDB(ctx context.Context) {
	var ok bool
	credentialsTableName, ok = os.LookupEnv("CREDENTIALS_TABLE_NAME")
	if !ok {
		panic("Could not find credentials table name")
	}
	cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion("us-east-1"))
	if err != nil {
		panic("unable to load SDK config, " + err.Error())
	}
	ddbClient = dynamodb.NewFromConfig(cfg)
}

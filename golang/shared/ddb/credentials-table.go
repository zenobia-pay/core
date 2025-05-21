package ddb

import (
	"context"
	"fmt"
	"os"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb"
	"github.com/aws/aws-sdk-go-v2/service/dynamodb/types"
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

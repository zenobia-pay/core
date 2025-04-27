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

func GetHashedRefreshTokenFromCredentialsTable(ctx context.Context, sub string) (string, error) {
	out, err := ddbClient.GetItem(ctx, &dynamodb.GetItemInput{
		TableName: aws.String(credentialsTableName),
		Key: map[string]types.AttributeValue{
			"pk": &types.AttributeValueMemberS{Value: sub},
		},
	})

	if err != nil {
		panic(fmt.Errorf("unexpected ddb error: %w", err))
	}

	if len(out.Item) == 0 {
		fmt.Printf("Could not get ddb item %s\n", err)
		return "", err
	}

	refreshToken, ok := out.Item["hashedRefreshToken"].(*types.AttributeValueMemberS)
	if !ok {
		panic("unexpected type for credentials item hashedRefreshToken")
	}
	return refreshToken.Value, nil
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

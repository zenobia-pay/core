package secrets

import (
	"context"
	"encoding/json"
	"log"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
)

var secretsClient *secretsmanager.Client
var refreshTokenHashingSecretName = "credentials/refreshtoken/hmac"
var jwtSigningSecretName = "jwt/customer/hmac"

func GetRefreshTokenHashingSecret(ctx context.Context) string {
	println("Fetching refresh token hashing secret")
	return getHmacSecret(ctx, refreshTokenHashingSecretName)
}

func GetJwtTokenHashingSecret(ctx context.Context) string {
	println("Fetching jwt signer hmac secret")
	return getHmacSecret(ctx, jwtSigningSecretName)
}

func getHmacSecret(ctx context.Context, secretId string) string {
	out, err := secretsClient.GetSecretValue(ctx, &secretsmanager.GetSecretValueInput{
		SecretId: aws.String(secretId),
	})
	if err != nil {
		log.Fatalf("failed to get secret: %v", err)
	}
	var data map[string]string
	if err := json.Unmarshal([]byte(*out.SecretString), &data); err != nil {
		panic(err)
	}
	return data["secret"]
}

func InitSecretsClient(ctx context.Context) {
	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		panic("unable to load SDK config, " + err.Error())
	}
	secretsClient = secretsmanager.NewFromConfig(cfg)
}

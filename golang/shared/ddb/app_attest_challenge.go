package ddb

// AppAttestChallenge represents a challenge item stored in DynamoDB for App Attest
// PK: Partition key (constant string, e.g., "APP_ATTEST_CHALLENGE")
// SK: Sort key (request ID or unique identifier)
// Challenge: the challenge string
// TTL: Unix timestamp for DynamoDB TTL
type AppAttestChallenge struct {
	PK        string `dynamodbav:"pk" json:"pk"`
	SK        string `dynamodbav:"sk" json:"sk"`
	Challenge string `dynamodbav:"challenge" json:"challenge"`
	TTL       int64  `dynamodbav:"ttl" json:"ttl"`
}

func GeneratePk(keyId string) string {
	return "APP_ATTEST#k_" + keyId
}

func GenerateSk() string {
	return "DETAILS"
}

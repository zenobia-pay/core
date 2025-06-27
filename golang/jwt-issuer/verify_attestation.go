package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"zenobia/shared/ddb"

	"github.com/aws/aws-lambda-go/events"
)

type VerifyAttestationRequest struct {
	AttestationObject string `json:"attestationObject"`
	ClientDataJSON    string `json:"clientDataJSON"`
	Challenge         string `json:"challenge"`
	KeyID             string `json:"keyId"`
}

type VerifyAttestationResponse struct {
	Status  string `json:"status"`
	Message string `json:"message"`
}

// HandleVerifyAttestation handles the /verify-attestation endpoint
func HandleVerifyAttestation(ctx context.Context, request events.APIGatewayProxyRequest) (events.APIGatewayProxyResponse, error) {
	// Parse the request body
	var req VerifyAttestationRequest
	if err := json.Unmarshal([]byte(request.Body), &req); err != nil {
		return apiError(http.StatusBadRequest, "Invalid request body"), nil
	}

	// Lookup the challenge in DynamoDB
	storedChallenge, err := ddb.GetStoredChallenge(ctx, request.RequestContext.RequestID)
	if err != nil {
		return apiError(http.StatusForbidden, "Challenge not found or expired"), nil
	}

	if storedChallenge != req.Challenge {
		return apiError(http.StatusForbidden, "Challenge mismatch"), nil
	}

	// Validate the attestation with Apple (stub for now)
	if err := validateAppleAttestation(req.AttestationObject, req.ClientDataJSON, req.Challenge, req.KeyID); err != nil {
		return apiError(http.StatusForbidden, fmt.Sprintf("Attestation validation failed: %v", err)), nil
	}

	// TODO: Store the attested key in DDB for future assertions
	// err = ddb.StoreAppAttestKey(ctx, req.KeyID, <userId>, "verified")
	// if err != nil {
	// 	return apiError(http.StatusInternalServerError, "Failed to store app attest key"), nil
	// }

	resp := VerifyAttestationResponse{
		Status:  "success",
		Message: "Attestation verified successfully",
	}
	respBody, _ := json.Marshal(resp)
	return events.APIGatewayProxyResponse{
		StatusCode: 200,
		Body:       string(respBody),
	}, nil
}

// validateAppleAttestation validates the attestation object with Apple
func validateAppleAttestation(attObjB64, clientDataJSONB64, challenge, keyId string) error {
	return nil
}

// apiError is a helper to return error responses
func apiError(status int, msg string) events.APIGatewayProxyResponse {
	resp := VerifyAttestationResponse{
		Status:  "error",
		Message: msg,
	}
	respBody, _ := json.Marshal(resp)
	return events.APIGatewayProxyResponse{
		StatusCode: status,
		Body:       string(respBody),
	}
}

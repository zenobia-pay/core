package main

import (
	"bytes"
	"context"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/sha256"
	"crypto/x509"
	"encoding/asn1"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"time"
	"zenobia/shared/ddb"

	"github.com/aws/aws-lambda-go/events"
	"github.com/fxamacker/cbor/v2"
)

const (
	appleTeamId   = "XZAH4FVXK7"
	appleBundleId = "zenobia.Zenobia"
)

type VerifyAttestationRequest struct {
	AttestationObject string `json:"attestationObject"`
	ClientDataJSON    string `json:"clientDataJSON"`
	Challenge         string `json:"challenge"`
	KeyID             string `json:"keyId"`
	RequestID         string `json:"requestId"`
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
	storedChallenge, err := ddb.GetStoredChallenge(ctx, req.RequestID)
	log.Println("Stored challenge: " + storedChallenge)
	if err != nil {
		return apiError(http.StatusForbidden, "Challenge not found or expired"), nil
	}

	if storedChallenge != req.Challenge {
		return apiError(http.StatusForbidden, "Challenge mismatch"), nil
	}

	// Validate the attestation with Apple (stub for now)
	if err := ValidateAppleAttestation(req.AttestationObject, req.Challenge, req.KeyID, appleTeamId, appleBundleId, req.ClientDataJSON, time.Now()); err != nil {
		log.Println("Attestation validation failed: " + err.Error())
		return apiError(http.StatusForbidden, fmt.Sprintf("Attestation validation failed: %v", err)), nil
	}
	log.Println("Attestation validation success")

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

const (
	// Apple's App Attestation root certificate URL
	appleRootCertURL = "https://www.apple.com/certificateauthority/Apple_App_Attestation_Root_CA.pem"
)

// AttestationObject represents the CBOR-encoded attestation object
type AttestationObject struct {
	Fmt      string                 `cbor:"fmt"`
	AuthData []byte                 `cbor:"authData"`
	AttStmt  map[string]interface{} `cbor:"attStmt"`
}

// AppAttestExtension is used for ASN.1 decoding of the App Attest extension
type AppAttestExtension struct {
	Nonce []byte `asn1:"explicit,tag:1"`
}

// AuthData represents the authenticator data structure
type AuthData struct {
	RPIDHash       []byte // 32 bytes
	Flags          byte
	Counter        uint32 // 4 bytes
	Aaguid         []byte // 16 bytes
	CredentialID   []byte // variable length
	CredentialData []byte // variable length, contains COSE public key
}

// ValidateAppleAttestation validates the attestation object with Apple
// see: https://developer.apple.com/documentation/devicecheck/validating-apps-that-connect-to-your-server
func ValidateAppleAttestation(attObjB64, challengeB64, keyId, teamId, bundleId, clientDataJSON string, currentTime time.Time) error {

	// fmt.Printf("att obj %s, challenge %s, key id %s, team id %s, bundle id %s, client data JSON %s", attObjB64, challengeB64, keyId, teamId, bundleId, clientDataJSON)
	attObjBytes, err := base64.StdEncoding.DecodeString(attObjB64)
	if err != nil {
		return fmt.Errorf("failed to decode attestation object: %w", err)
	}

	challenge, err := base64.StdEncoding.DecodeString(challengeB64)
	if err != nil {
		return fmt.Errorf("failed to decode challenge: %w", err)
	}

	// Decode CBOR attestation object
	var attestationObject AttestationObject
	if err := cbor.Unmarshal(attObjBytes, &attestationObject); err != nil {
		return fmt.Errorf("failed to unmarshal attestation object: %w", err)
	}

	if attestationObject.Fmt != "apple-appattest" {
		return fmt.Errorf("unexpected attestation format: %s", attestationObject.Fmt)
	}

	// Step 1: Verify the x5c array certificates
	x5cArray, ok := attestationObject.AttStmt["x5c"].([]interface{})
	if !ok || len(x5cArray) < 2 {
		return errors.New("invalid or missing x5c certificate array in attestation statement")
	}

	// Verify the certificate chain using Apple's App Attest root certificate
	if err := verifyCertificateChain(x5cArray, currentTime); err != nil {
		return fmt.Errorf("certificate chain validation failed: %w", err)
	}

	// Step 2: Append the raw challenge bytes to authenticator data
	// According to Apple's documentation, we should append the raw challenge bytes
	// directly to the authenticator data, not its hash
	nonceData := append(attestationObject.AuthData, challenge...)
	fmt.Println("Nonce Data: " + base64.StdEncoding.EncodeToString(nonceData[:]))

	// Step 3: Generate a new SHA256 hash of the composite item to create nonce
	nonce := sha256.Sum256(nonceData)
	fmt.Println("Nonce: " + base64.StdEncoding.EncodeToString(nonce[:]))

	// Step 4: Obtain and verify the credCert extension with OID 1.2.840.113635.100.8.2
	// Get the first certificate in the x5c array, which is the credCert
	credCertBytes, ok := x5cArray[0].([]byte)
	if !ok {
		return errors.New("invalid credential certificate format")
	}

	// Parse the certificate
	credCert, err := x509.ParseCertificate(credCertBytes)
	if err != nil {
		return fmt.Errorf("failed to parse credential certificate: %w", err)
	}

	// Apple App Attest extension OID: 1.2.840.113635.100.8.2
	appAttestOID := asn1.ObjectIdentifier{1, 2, 840, 113635, 100, 8, 2}

	// Find the extension with the App Attest OID
	var appAttestExtension []byte
	for _, ext := range credCert.Extensions {
		if ext.Id.Equal(appAttestOID) {
			appAttestExtension = ext.Value
			break
		}
	}

	fmt.Println("Extension data: " + base64.StdEncoding.EncodeToString(appAttestExtension))

	if appAttestExtension == nil {
		return errors.New("app attest extension not found in credential certificate")
	}

	fmt.Println("Extension data (base64): " + base64.StdEncoding.EncodeToString(appAttestExtension))

	// Decode the ASN.1 sequence and extract the explicitly tagged nonce
	var ext AppAttestExtension
	_, err = asn1.Unmarshal(appAttestExtension, &ext)
	if err != nil {
		return fmt.Errorf("failed to unmarshal inner nonce: %w", err)
	}
	nonceFromCert := ext.Nonce

	// Verify that the extracted octet string equals the nonce
	if !bytes.Equal(nonceFromCert, nonce[:]) {
		return errors.New("nonce verification failed: certificate nonce does not match computed nonce")
	}

	// Step 5: Create and verify the SHA256 hash of the public key in credCert
	// Extract the public key from the credential certificate
	pubKey, ok := credCert.PublicKey.(*ecdsa.PublicKey)
	if !ok {
		return errors.New("credential certificate public key is not an ECDSA key")
	}

	// Convert the public key to X9.62 uncompressed point format
	// Format: 0x04 (uncompressed) + X (32 bytes) + Y (32 bytes)
	pubKeyBytes := elliptic.Marshal(pubKey.Curve, pubKey.X, pubKey.Y)

	// Create SHA256 hash of the public key
	pubKeyHash := sha256.Sum256(pubKeyBytes)
	fmt.Printf("public key hash: %s\n", base64.StdEncoding.EncodeToString(pubKeyHash[:]))

	// Convert the key ID from base64 to bytes for comparison
	keyIDBytes, err := base64.StdEncoding.DecodeString(keyId)
	if err != nil {
		return fmt.Errorf("failed to decode key ID: %w", err)
	}

	// Verify that the hash matches the key identifier from the app
	if !bytes.Equal(pubKeyHash[:], keyIDBytes) {
		return errors.New("public key hash does not match the provided key ID")
	}

	// Steps 6 & 7: Parse the authenticator data into a structured object
	authData, err := parseAuthData(attestationObject.AuthData)
	if err != nil {
		return fmt.Errorf("failed to parse authenticator data: %w", err)
	}

	// Step 6: Verify the authenticator data's RP ID hash against the hash of the app's App ID
	appID := teamId + "." + bundleId
	fmt.Println("Using App ID: " + appID)
	appIDHash := sha256.Sum256([]byte(appID))
	fmt.Println("App ID Hash: " + base64.StdEncoding.EncodeToString(appIDHash[:]))

	// Verify that the RP ID hash in the authenticator data matches the App ID hash
	if !bytes.Equal(authData.RPIDHash, appIDHash[:]) {
		return fmt.Errorf("RP ID hash mismatch: expected %x, got %x", appIDHash[:], authData.RPIDHash)
	}

	// Step 7: Verify that the authenticator data's counter field equals 0
	// For initial attestations in App Attest, the counter must be 0
	if authData.Counter != 0 {
		return fmt.Errorf("counter verification failed: expected 0, got %d", authData.Counter)
	}

	// Step 8: Verify the authenticator data's aaguid field
	// Check if the aaguid field is present
	if authData.Aaguid == nil || len(authData.Aaguid) != 16 {
		return errors.New("missing or invalid aaguid field in authenticator data")
	}

	// Define the expected aaguid values
	// For development environment: "appattestdevelop"
	// For production environment: "appattest" followed by seven 0x00 bytes
	devAAGUID := []byte("appattestdevelop")
	prodAAGUID := append([]byte("appattest"), bytes.Repeat([]byte{0x00}, 7)...)

	if !bytes.Equal(authData.Aaguid, devAAGUID) && !bytes.Equal(authData.Aaguid, prodAAGUID) {
		return fmt.Errorf("invalid aaguid: expected either %x or %x, got %x", devAAGUID, prodAAGUID, authData.Aaguid)
	}

	// Step 9: Verify that the authenticator data's credentialId field matches the key identifier
	if authData.CredentialID == nil {
		return errors.New("missing credentialId field in authenticator data")
	}

	fmt.Printf("Credential ID: %s\n", base64.StdEncoding.EncodeToString(authData.CredentialID))

	// Verify that the credentialId matches the key identifier
	if !bytes.Equal(authData.CredentialID, keyIDBytes) {
		return fmt.Errorf("credentialId does not match the key identifier: expected %x, got %x", keyIDBytes, authData.CredentialID)
	}

	return nil
}

// parseAuthData parses the authenticator data structure
func parseAuthData(data []byte) (*AuthData, error) {
	// Authenticator data must be at least 37 bytes
	// - 32 bytes for RP ID hash
	// - 1 byte for flags
	// - 4 bytes for counter
	if len(data) < 37 {
		return nil, errors.New("authenticator data too short")
	}

	// Create a new AuthData object
	authData := &AuthData{
		RPIDHash: data[:32],
		Flags:    data[32],
	}

	// Extract counter (4 bytes starting at index 33)
	authData.Counter = binary.BigEndian.Uint32(data[33:37])

	// If the AT flag (bit 6) is set, then attestation data is included
	// The AT flag is the 7th bit (0-indexed) of the flags byte
	if (authData.Flags & (1 << 6)) != 0 {
		// Attestation data includes:
		// - 16 bytes for AAGUID
		// - 2 bytes for credential ID length (L)
		// - L bytes for credential ID
		// - Variable bytes for credential public key (CBOR encoded)
		if len(data) < 55 { // 37 + 16 + 2
			return nil, errors.New("authenticator data too short for attestation data")
		}

		// Extract AAGUID (16 bytes)
		authData.Aaguid = data[37:53]

		// Extract credential ID length (2 bytes)
		credIDLen := binary.BigEndian.Uint16(data[53:55])

		// Check if there's enough data for credential ID
		if len(data) < 55+int(credIDLen) {
			return nil, errors.New("authenticator data too short for credential ID")
		}

		// Extract credential ID
		authData.CredentialID = data[55 : 55+credIDLen]

		// Extract credential data (remaining bytes)
		authData.CredentialData = data[55+credIDLen:]
	}

	return authData, nil
}

// verifyCertificateChain verifies the certificate chain against Apple's root certificate
func verifyCertificateChain(x5c []interface{}, currentTime time.Time) error {
	// Fetch Apple's WebAuthn Root CA certificate
	resp, err := http.Get(appleRootCertURL)
	if err != nil {
		log.Fatalf("failed to fetch Apple WebAuthn Root CA: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		log.Fatalf("failed to fetch Apple WebAuthn Root CA, status code: %d", resp.StatusCode)
	}

	rootCertBytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read Apple WebAuthn Root CA: %w", err)
	}

	block, _ := pem.Decode(rootCertBytes)
	if block == nil || block.Type != "CERTIFICATE" {
		log.Fatal("failed to decode PEM block containing certificate")
	}

	rootCert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return fmt.Errorf("failed to parse Apple WebAuthn Root CA: %w", err)
	}

	// Create a certificate pool and add the root certificate
	rootPool := x509.NewCertPool()
	rootPool.AddCert(rootCert)

	// Build the certificate chain
	certs := make([]*x509.Certificate, 0, len(x5c))
	for _, certBytes := range x5c {
		certBytesTyped, ok := certBytes.([]byte)
		if !ok {
			return errors.New("invalid certificate in chain")
		}

		cert, err := x509.ParseCertificate(certBytesTyped)
		fmt.Printf("Certificate in chain: %s\n\n", base64.StdEncoding.EncodeToString(cert.Raw))
		if err != nil {
			return fmt.Errorf("failed to parse certificate in chain: %w", err)
		}

		certs = append(certs, cert)
	}

	// The first certificate is the leaf certificate
	leafCert := certs[0]

	// Create an intermediate certificate pool with any intermediate certificates
	intermediatePool := x509.NewCertPool()
	for i := 1; i < len(certs); i++ {
		intermediatePool.AddCert(certs[i])
	}

	// Verify the certificate chain
	_, err = leafCert.Verify(x509.VerifyOptions{
		Roots:         rootPool,
		Intermediates: intermediatePool,
		CurrentTime:   currentTime,
		KeyUsages:     []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth},
	})

	return err
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

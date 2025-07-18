import requests
import os
import json
import base64
from collections import OrderedDict
from hashlib import sha256
import ecdsa
from asn1crypto.algos import DSASignature
from .auth import get_merchant_token, get_customer_token

class MerchantApi:
    def __init__(self):
        self.base_url = os.environ.get('API_ENDPOINT')
        self.token = get_merchant_token()
        
    def create_transfer_request(self, amount, statement_items=None, transfer_metadata=None):
        """Create a new transfer request
        
        Args:
            amount: Integer amount in cents
            statement_items: List of statement items (optional)
            transfer_metadata: Dictionary of metadata (optional)
        """
        if statement_items is None:
            statement_items = []
        if transfer_metadata is None:
            transfer_metadata = {}
            
        return requests.post(
            f"{self.base_url}/create-transfer-request",
            headers={"Authorization": f"Bearer {self.token}"},
            json={
                "amount": amount,
                "statementItems": statement_items,
                "transferMetadata": transfer_metadata
            }
        )

class CustomerApi:
    def __init__(self):
        self.base_url = os.environ.get('API_ENDPOINT')
        self.device_id = os.environ.get('DEVICE_ID')
        self.token = get_customer_token()
        
    def list_bank_accounts(self):
        """List bank accounts for a customer"""
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        return requests.post(
            f"{self.base_url}/list-bank-accounts",
            headers=headers,
            json={
                "deviceId": self.device_id
            }
        )
    
    def get_customer_transfer(self, transfer_id, authed=True):
        """Get a customer transfer (with or without auth)
        
        Args:
            transfer_id: ID of the transfer to retrieve
            authed: Whether to include authentication token (default: True)
        """
        print("Token: ", self.token)
        print("Transfer id: ", transfer_id)
        headers = {"Authorization": f"Bearer {self.token}"} if authed and self.token else {"Authorization": "NONE"}
        params = {"id": transfer_id}
        return requests.get(
            f"{self.base_url}/get-customer-transfer",
            headers=headers,
            params=params
        )
    
    def list_customer_transfers(self, continuation_token=None):
        """List transfers for a customer
        
        Args:
            continuation_token: Token for pagination (optional)
        """
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        json_body = {}
        if continuation_token:
            json_body["continuationToken"] = continuation_token
            
        return requests.post(
            f"{self.base_url}/list-customer-transfers",
            headers=headers,
            json=json_body
        )
        
    def fulfill_transfer(self, transfer_id, bank_account_id):
        """Fulfill a transfer with proper request signing
        
        Args:
            transfer_id: ID of the transfer to fulfill
            bank_account_id: ID of the bank account to use
        """
        # Create request body with all fields except signature
        request_body = {
            "transferRequestId": transfer_id,
            "bankAccountId": bank_account_id,
            "deviceId": self.device_id
        }
        
        # Sort fields alphabetically for signature calculation
        sorted_body = OrderedDict(sorted(request_body.items()))
        
        # Convert to JSON string for signing
        json_body = json.dumps(sorted_body, separators=(',', ':'))
        
        # Generate signature using private key from environment variable
        # Expecting base64 encoded private key
        signature = self.sign_with_private_key(json_body)
        
        # Add the signature to the request body after sorting/serializing
        request_body["signature"] = signature
            
        headers = {"Authorization": f"Bearer {self.token}"} if self.token else {}
        return requests.post(
            f"{self.base_url}/fulfill-transfer",
            headers=headers,
            json=request_body
        )

    def sign_with_private_key(self, data):
        encoded_private_key = os.environ.get('CUSTOMER_PRIVATE_KEY')
        if not encoded_private_key:
            raise ValueError("CUSTOMER_PRIVATE_KEY environment variable is required for signing")
        try:
            # Decode the base64 encoded private key
            decoded_private_key = base64.b64decode(encoded_private_key)
            print("Decoded key: ", decoded_private_key)
            
            # Parse the private key using ecdsa
            private_key = ecdsa.SigningKey.from_string(decoded_private_key, curve=ecdsa.NIST256p)
            
            # Sign the data using raw R||S format first
            data_to_sign = data.encode('utf-8')
            raw_signature = private_key.sign_deterministic(
                data_to_sign,
                hashfunc=sha256,
                sigencode=ecdsa.util.sigencode_string  # Use raw R||S encoding
            )
            
            # Convert raw signature to ASN.1/DER format (X9.62 format used by iOS)
            r_int = int.from_bytes(raw_signature[:32], byteorder='big')
            s_int = int.from_bytes(raw_signature[32:], byteorder='big')
            der_signature = DSASignature({'r': r_int, 's': s_int}).dump()
            
            print("Raw signature bytes: ", raw_signature)
            print("DER encoded signature bytes: ", der_signature)
            
            # Convert signature to base64
            sig_b64 = base64.b64encode(der_signature).decode('utf-8')
            print("Sig b64: ", sig_b64)
            
            # Create signature object
            signature = {
                "signatureType": "SHA256_WITH_ECDSA",
                "signatureValue": sig_b64
            }
        except Exception as e:
            raise Exception(f"Failed to sign request: {str(e)}")
        return signature

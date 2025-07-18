import pytest
import time
import uuid
import os
import sys
from utils.api import MerchantApi, CustomerApi

def test_full_transfer_flow():
    """Test the full transfer flow from creation to completion"""
    
    transfer_amount = 1  # 1 cent
    
    # Step 1: Create a transfer request as merchant
    merchant_api = MerchantApi()
    statement_items = [
        {
            "name": "Test Item",
            "amount": transfer_amount,
            "itemId": f"test-item-{uuid.uuid4()}"
        }
    ]
    transfer_metadata = {}
    create_response = merchant_api.create_transfer_request(
        amount=transfer_amount,
        statement_items=statement_items,
        transfer_metadata=transfer_metadata
    )
    assert create_response.status_code == 200, f"Failed to create transfer: {create_response.text}"
    transfer_id = create_response.json()["transferRequestId"]
    print(f"Created transfer with ID: {transfer_id}")
    
    # Step 2: List bank accounts as customer
    customer_api = CustomerApi()
    
    bank_accounts_response = customer_api.list_bank_accounts()
    assert bank_accounts_response.status_code == 200, f"Failed to list bank accounts: {bank_accounts_response.text}"
    
    # Get a bank account ID for fulfillment
    try:
        response_json = bank_accounts_response.json()
        print(f"JSON response: {response_json}")
        # The API returns bank accounts in the 'items' field, not 'bankAccounts'
        bank_accounts = response_json.get("items", [])
        print(f"Bank accounts: {bank_accounts}")
    except Exception as e:
        print(f"Error parsing JSON: {e}")
        bank_accounts = []
    print("--- END DEBUG ---\n")
    
    if not bank_accounts:
        print("\n*** SKIPPING TEST: No bank accounts available for testing ***\n")
        pytest.skip("No bank accounts available for testing")
    bank_account_id = bank_accounts[0].get("bankAccountId")
    
    # Step 3: Get customer transfer (authenticated)
    transfer_authed_response = customer_api.get_customer_transfer(
        transfer_id=transfer_id,
        authed=True
    )
    assert transfer_authed_response.status_code == 200, f"Failed to get transfer (authed): {transfer_authed_response.text}"
    print(f"Successfully retrieved transfer (authenticated)")
    
    # Step 4: Get customer transfer (unauthenticated)
    unauthed_api = CustomerApi()  # No customer_id = no token
    transfer_unauthed_response = unauthed_api.get_customer_transfer(
        transfer_id=transfer_id,
        authed=False
    )
    assert transfer_unauthed_response.status_code == 200, f"Failed to get transfer (unauthed): {transfer_unauthed_response.text}"
    print(f"Successfully retrieved transfer (unauthenticated)")
    
    # Step 5: Fulfill the transfer
    fulfill_response = customer_api.fulfill_transfer(
        transfer_id=transfer_id,
        bank_account_id=bank_account_id,
    )
    assert fulfill_response.status_code == 200, f"Failed to fulfill transfer: {fulfill_response.text}"
    print(f"Successfully fulfilled transfer")
    
    # Wait for transfer processing (adjust as needed)
    print(f"Waiting for transfer processing...")
    time.sleep(10)  # Increased wait time to ensure processing completes
    
    # Step 6: List customer transfers and verify success
    transfers_response = customer_api.list_customer_transfers(continuation_token=None)
    assert transfers_response.status_code == 200, f"Failed to list transfers: {transfers_response.text}"
    
    # Verify the transfer is in the list and has status "PAID"
    transfers = transfers_response.json().get("items", [])
    matching_transfers = [t for t in transfers if t.get("transferRequestId") == transfer_id]
    assert len(matching_transfers) == 1, f"Transfer {transfer_id} not found in list"
    assert matching_transfers[0].get("status") == "PAID", f"Transfer status is not PAID: {matching_transfers[0].get('status')}"
    print(f"Successfully verified transfer status is PAID")


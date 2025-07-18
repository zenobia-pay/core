import requests
import os
import json

def get_merchant_token():
    """Get an M2M token for merchant API access"""
    auth0_domain = os.environ.get('AUTH0_DOMAIN')
    client_id = os.environ.get('AUTH0_M2M_MERCHANT_CLIENT_ID')
    client_secret = os.environ.get('AUTH0_M2M_MERCHANT_CLIENT_SECRET')
    audience = "https://dashboard.zenobiapay.com"
    
    response = requests.post(
        f"https://{auth0_domain}/oauth/token",
        headers={"content-type": "application/json"},
        data=json.dumps({
            "client_id": client_id,
            "client_secret": client_secret,
            "audience": audience,
            "grant_type": "client_credentials"
        })
    )
    return response.json()["access_token"]

def get_customer_token():
    """Get a token for customer API access using the issue-jwt endpoint"""
    # Get environment variables
    api_endpoint = os.environ.get('API_ENDPOINT')
    sub = os.environ.get('CUSTOMER_SUB')
    refresh_token = os.environ.get('CUSTOMER_REFRESH_TOKEN')
    
    # Check if we have the required variables
    if not (api_endpoint and sub and refresh_token):
        print("Warning: Missing required environment variables for customer authentication.")
        print("Using merchant token as fallback. This may cause test failures.")
    
    # Call the issue-jwt endpoint to get a JWT for the customer
    try:
        response = requests.post(
            f"{api_endpoint}/issue-jwt",
            headers={
                "content-type": "application/json",
                "authorization": "NONE"
            },
            json={
                "refreshToken": refresh_token,
                "sub": sub
            }
        )
        
        if response.status_code != 200:
            print(f"Error getting customer token: {response.status_code} - {response.text}")
            return None
            
        return response.json()["jwt"]
    except Exception as e:
        print(f"Exception getting customer token: {str(e)}")
        # Fall back to merchant token if we can't get a customer token
        return get_merchant_token()

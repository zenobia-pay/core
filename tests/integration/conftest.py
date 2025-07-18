import os
import pytest
import boto3
from dotenv import load_dotenv

def pytest_sessionstart(session):
    """
    Called after the Session object has been created and before tests are executed.
    """
    # Load environment variables from .env file if it exists (for local testing)
    load_dotenv()
    
    # Verify required environment variables
    required_vars = [
        'API_ENDPOINT',
        'AUTH0_DOMAIN',
        'AUTH0_M2M_MERCHANT_CLIENT_ID',
        'AUTH0_M2M_MERCHANT_CLIENT_SECRET',
    ]
    
    # Check for either M2M credentials or customer refresh token
    customer_auth_vars = ['CUSTOMER_SUB', 'CUSTOMER_REFRESH_TOKEN']
    if not any(os.environ.get(var) for var in customer_auth_vars):
        print("Warning: No customer authentication variables found. Tests may fail if they require customer authentication.")
        print("Consider setting CUSTOMER_SUB and CUSTOMER_REFRESH_TOKEN in your .env file.")
    
    missing = [var for var in required_vars if not os.environ.get(var)]
    if missing:
        pytest.exit(f"Missing required environment variables: {', '.join(missing)}")

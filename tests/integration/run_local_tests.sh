#!/bin/bash

# Activate virtual environment if not already activated
if [[ "$VIRTUAL_ENV" == "" ]]; then
  echo "Activating virtual environment..."
  source ../../.venv/bin/activate
fi

# Check if .env file exists, if not, create from template
if [ ! -f ".env" ]; then
  echo "No .env file found. Creating from template..."
  if [ -f ".env.template" ]; then
    cp .env.template .env
    echo "Please edit .env file with your actual values before running tests."
    exit 1
  else
    echo "Error: .env.template not found. Please create a .env file manually."
    exit 1
  fi
fi

# Run the tests (python-dotenv will automatically load .env file)
# Adding -s flag to show print statements
python -m pytest test_transfer_flow.py -v -s

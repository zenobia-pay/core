#!/bin/bash

# Get the RDS endpoint from AWS
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --query "DBInstances[?DBInstanceIdentifier=='metadatadb-dev'].Endpoint.Address" \
  --output text \
  --profile $2)

echo "Connecting to RDS endpoint: $RDS_ENDPOINT"

aws ssm start-session \
  --target $1 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$RDS_ENDPOINT\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"5432\"]}" \
  --profile $2

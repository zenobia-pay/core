#!/bin/bash

# Get the RDS endpoint from AWS
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --query "DBInstances[?DBInstanceIdentifier=='metadatadb'].Endpoint.Address" \
  --output text \
  --profile $1)

INSTANCE_TARGET_ID=$(aws ssm describe-instance-information \
  --query "InstanceInformationList[].InstanceId" \
  --output text \
  --profile $1)

echo "Connecting to RDS endpoint: $RDS_ENDPOINT"

aws ssm start-session \
  --target $INSTANCE_TARGET_ID \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$RDS_ENDPOINT\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"5432\"]}" \
  --profile $1

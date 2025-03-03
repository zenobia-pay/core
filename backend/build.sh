#!/bin/bash
set -e

echo "Building Kotlin project..."
./gradlew clean build

echo "Building with SAM..."
sam build

echo "Starting API..."
sam local start-api \
   --docker-network backend_sam-local \
   --env-vars env.json \
   --warm-containers LAZY
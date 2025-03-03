#!/bin/bash
set -e

# Function to clean up background processes on exit
cleanup() {
    echo "Cleaning up..."
    pkill -f "sam local start-api" || true
    kill $(jobs -p) 2>/dev/null || true
}

# Set up cleanup on script exit
trap cleanup EXIT

# Initial build
echo "Initial build..."
./gradlew clean build
sam build

# Start SAM in the background
echo "Starting SAM API..."
sam local start-api \
   --docker-network backend_sam-local \
   --env-vars env.json \
   --warm-containers LAZY &

# Watch for changes and rebuild
echo "Watching for changes..."
while true; do
    fswatch -1 src/
    echo "Changes detected, rebuilding..."
    ./gradlew build
    sam build
    pkill -f "sam local start-api" || true
    echo "Restarting SAM API..."
    sam local start-api \
      --docker-network backend_sam-local \
      --env-vars env.json \
      --warm-containers LAZY &
    echo "Ready for more changes..."
done
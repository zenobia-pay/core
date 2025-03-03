#!/bin/bash
set -e

echo "Cleaning up Docker resources..."

# Stop and remove all containers
echo "Stopping all Docker containers..."
docker stop $(docker ps -a -q) 2>/dev/null || true

echo "Removing all Docker containers..."
docker rm $(docker ps -a -q) 2>/dev/null || true

# Remove the SAM network
echo "Removing Docker network..."
docker network rm backend_sam-local 2>/dev/null || true

# Clean up any dangling Docker resources
echo "Cleaning up dangling Docker resources..."
docker system prune -f

echo "Cleanup complete! You can now run ./build.sh to start fresh."
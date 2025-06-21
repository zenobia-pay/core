# Item Metadata Handler Test Inputs

This directory contains test inputs for the ItemMetadataHandler Lambda function that processes messages from the TransferMetadataQueue.

## Available Test Inputs

1. `item-metadata-handler-put.json` - Tests the PUT operation for storing new item metadata
2. `item-metadata-handler-update.json` - Tests the UPDATE operation for updating item ownership

## How to Use

You can invoke the Lambda function locally using the AWS SAM CLI with these input files:

```bash
# For PUT operation
sam local invoke TransferMetadataRecorderFunction -e sam/inputs/item-metadata-handler-put.json

# For UPDATE operation
sam local invoke TransferMetadataRecorderFunction -e sam/inputs/item-metadata-handler-update.json
```

## Input Structure

### PUT Operation
The PUT operation stores transfer and item metadata in the database. The input includes:
- merchantId: The ID of the merchant
- transferRequestId: The ID of the transfer request
- creationTime: When the transfer was created
- transferMetadata: Additional metadata about the transfer
- itemMetadata: Metadata for each item in the transfer

### UPDATE Operation
The UPDATE operation updates ownership information for all items in a transfer. The input includes:
- transferRequestId: The ID of the transfer request
- ownerId: The ID of the new owner
- ownershipTime: When ownership was transferred

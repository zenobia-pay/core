# ZenobiaPay Backend

A Kotlin-based serverless backend using AWS SAM and DynamoDB.

## Prerequisites

Make sure you have the following installed:

- Java 17 (OpenJDK)

  ```bash
  brew install openjdk@17
  ```

- AWS SAM CLI

  ```bash
  brew install aws-sam-cli
  ```

- fswatch (for development)

  ```bash
  brew install fswatch
  ```

- Docker Desktop (for local DynamoDB)

## Getting Started

`brew install gradle` if you don't have it already.

cd `backend/` and run `gradle wrapper` to generate the gradle wrapper files.

## Project Structure

```
backend/
├── src/main/kotlin/com/zenobiapay/
│   ├── handlers/           # Lambda function handlers
│   └── model/             # Data models
├── build.gradle.kts       # Gradle build configuration
├── template.yaml          # SAM template
├── docker-compose.yaml    # Local DynamoDB configuration
├── build.sh              # Build and run script
└── dev.sh               # Development script with auto-reload
```

## Getting Started

1. Clone the repository and navigate to the backend directory:

   ```bash
   cd backend
   ```

2. Launch docker desktop to initialize Docker

3. Start the local DynamoDB:

   ```bash
   docker-compose up -d
   ```

4. Create the DynamoDB table locally:
   ```bash
   aws dynamodb create-table \
       --table-name SampleTable \
       --attribute-definitions AttributeName=id,AttributeType=S \
       --key-schema AttributeName=id,KeyType=HASH \
       --provisioned-throughput ReadCapacityUnits=2,WriteCapacityUnits=2 \
       --endpoint-url http://localhost:8000
   ```

## Development

For development with auto-reload (recommended):

```bash
./dev.sh
```

This will:

- Build the project
- Start the API
- Watch for changes
- Automatically rebuild and restart when files change

For a one-time build and run:

```bash
./build.sh
```

## API Endpoints

The API will be available at `http://localhost:3000` with the following endpoints:

- `GET /items` - Get all items
- `GET /items/{id}` - Get item by ID
- `POST /items` - Create new item

Example POST request:

```bash
curl -X POST http://localhost:3000/items \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Item", "price": 99.99, "description": "A test item"}'
```

## Deployment

To deploy to AWS:

```bash
sam deploy --guided
```

This will:

1. Package your application
2. Upload it to AWS
3. Create/update the CloudFormation stack
4. Deploy your API

## Development Notes

- The project uses Kotlin with AWS Lambda functions
- DynamoDB is used for data storage
- Local development uses DynamoDB Local for testing
- Auto-reload is enabled during development
- CORS is configured for all endpoints

## Troubleshooting

1. If DynamoDB connection fails:

   - Ensure Docker is running
   - Check if DynamoDB container is up: `docker ps`
   - Verify table creation: `aws dynamodb list-tables --endpoint-url http://localhost:8000`

2. If changes aren't reflecting:

   - Stop the current process
   - Run `./build.sh` or `./dev.sh` again

3. If build fails:

   - Ensure Java 17 is installed and set correctly
   - Try cleaning the build: `./gradlew clean`

     04.13.25 How to develop locally, lambda stack

Make changes to the lambda stack file for the args, lambda-stack.yml, and the corresponsding handler. eg for submit-customer-onboarding you see the route is defined in the UserHandler, which sends us to SubmitCustomerOnboardingOperation which is a class with a "run" handler. Make the necessary changes.

Then run `make openapi` which writes the changes from the yml file to the openapi spec.

Then run `make` (or just `make kotlin` if you're only changing kotlin files) which will read from the openapi spec.

Then go to zenobia.awsapps.com/start/# and find the `teddyli sandbox` account (talk to us if you don't see it) and click access keys. Go to Option 1: Set AWS environment variables. Copy and paste those.

Then run sam local. So for example to run the user handler lambda with the submit-customer-onboarding event as args call `sam local invoke UserHandlerFunction -e sam/inputs/submit-customer-onboarding.json --env-vars sam/inputs/env.json`

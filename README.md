# ZenobiaPay Backend

A Kotlin-based serverless backend using AWS SAM, DynamoDB, and AWS SES for email notifications. This system handles payment processing, merchant onboarding, and customer notifications through a comprehensive event-driven architecture.

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

## System Architecture

The ZenobiaPay backend is built on a serverless architecture using AWS services and follows event-driven design principles:

### Core Components

1. **Lambda Functions**: Kotlin-based serverless functions that handle specific business logic
   - User handlers for customer onboarding
   - Transfer event handlers for payment processing
   - Notification handlers for email and webhook communications

2. **DynamoDB**: NoSQL database for storing application data
   - Transfer table with DynamoDB streams for event propagation
   - User table for merchant and customer information

3. **Event-Driven Processing**:
   - DynamoDB streams capture table changes
   - EventBridge pipes route events to appropriate Lambda functions
   - SQS queues for asynchronous processing and retries

4. **Notification System**:
   - AWS SES for email notifications to merchants
   - Webhook system for real-time status updates
   - JWT signing for secure webhook payloads

### Deployment Infrastructure

- **AWS SAM**: Infrastructure as Code for AWS resources
- **GitHub Actions**: CI/CD pipeline for automated deployments
- **Terraform**: Additional infrastructure management for specific resources

## Project Structure

```
core/
├── kotlin/                           # Kotlin source code
│   ├── lambda/                       # Lambda function implementations
│   │   ├── payout-handler/           # Payout processing logic
│   │   ├── transfer-event-handler/   # Transfer event processing
│   │   │   ├── src/main/kotlin/com/zenobiapay/transfertableevent/
│   │   │   │   ├── di/               # Dependency injection modules
│   │   │   │   ├── handlers/         # Lambda function handlers
│   │   │   │   ├── logic/            # Business logic implementation
│   │   │   │   └── util/             # Utility classes (including EmailUtil)
│   │   │   └── build.gradle.kts      # Module-specific dependencies
│   └── common/                       # Shared Kotlin code
├── gradle/
│   └── libs.versions.toml           # Centralized dependency version management
├── sam/                              # SAM template files
│   ├── lambda-stack.yml             # Lambda function definitions
│   └── payout-stack.yml             # Payout infrastructure stack
├── .github/workflows/               # GitHub Actions workflows
│   └── deploy-infra.yml             # Deployment automation
├── template.yml                     # Main SAM template
└── build.gradle.kts                 # Root Gradle build file
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

### Dependency Management

The project uses Gradle with Kotlin DSL for build configuration and dependency management:

- **libs.versions.toml**: Centralized version catalog located in `gradle/libs.versions.toml`
  - Defines all dependency versions in a single place
  - Provides version aliases that can be referenced in build.gradle.kts files
  - Example: `aws-ses = { module = "software.amazon.awssdk:ses", version.ref = "aws" }`

- **Module-specific build files**: Each Lambda module has its own build.gradle.kts
  - References dependencies from the central catalog using the `libs` accessor
  - Example: `implementation(libs.aws.ses)` to include AWS SES

### CI/CD Pipeline

The project uses GitHub Actions for continuous integration and deployment:

- **deploy-infra.yml**: Main workflow file in `.github/workflows/`
  - Triggered on pushes to `beta` and `prod` branches
  - Builds Kotlin, Go, and Node.js artifacts
  - Deploys infrastructure using AWS SAM
  - Passes environment variables to deployed services

- **Environment Variables**:
  - Stored as GitHub repository variables and secrets
  - Passed to SAM templates during deployment
  - Example: `SENDER_EMAIL` for notification sender address

### SAM Deployment Architecture

- **template.yml**: Main SAM template that defines:
  - Parameters that can be passed during deployment
  - Nested stacks for different components
  - Resource permissions and configurations

- **Nested Stacks**:
  - **payout-stack.yml**: Handles payout processing and notifications
  - **lambda-stack.yml**: Defines API Gateway and Lambda functions

### Email Notification System

The system includes an email notification feature using AWS SES:

- **EmailUtil.kt**: Utility class for sending formatted emails
  - Uses AWS SES for email delivery
  - Supports HTML-formatted emails with dynamic content
  - Sends notifications for specific transfer status changes

- **Configuration**:
  - Sender email is configured via environment variable `SENDER_EMAIL`
  - Email templates are defined in the code with status-specific formatting
  - Integration with TransferTableEventLogic for automatic notifications

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

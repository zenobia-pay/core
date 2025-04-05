GO_LAMBDA_DIRS := authorizer

.PHONY: all go kotlin clean build

all: go kotlin

go:
	@for dir in $(GO_LAMBDA_DIRS); do \
		echo "Building $$dir..."; \
		docker run --rm -v "$(PWD)":/app -w /app/golang/$$dir public.ecr.aws/amazonlinux/amazonlinux:2 \
		bash -c 'yum install -y golang zip && \
		         GOOS=linux GOARCH=amd64 go build -buildvcs=false -o ./build/bootstrap . && \
				 cd build && \
		         zip function.zip bootstrap'; \
	done

go-dev:
	echo "Building golang (dev mode). Build in container if you'd like to deploy"
	@for dir in $(GO_LAMBDA_DIRS); do \
 		echo "Building $$dir..."; \
 		cd golang/$$dir && GOOS=linux GOARCH=amd64 go build -o ./build/bootstrap . && cd build && zip function.zip bootstrap && cd ../../..; \
 	done

kotlin:
	echo "Building gradle"
	./gradlew build --parallel --no-daemon

kotlin-dev:
	echo "Building gradle (dev mode)"
	./gradlew build

openapi:
	yq eval '.Resources.ZenobiaApi.Properties.DefinitionBody' sam/lambda-stack.yml | sed -E 's/!Sub //g' > openapi.yml

clean:
	echo "Cleaning kotlin"
	./gradlew clean

	echo "Cleaning go"
	@for dir in $(GO_LAMBDA_DIRS); do \
		echo "Cleaning $$dir..."; \
		rm -rf ./golang/$$dir/build; \
	done

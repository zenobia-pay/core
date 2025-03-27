GO_LAMBDA_DIRS := authorizer

.PHONY: all go kotlin clean build

all: go kotlin

go:
	@for dir in $(GO_LAMBDA_DIRS); do \
		echo "Building $$dir..."; \
		docker run --rm -v "$(PWD)":/app -w /app/golang/$$dir public.ecr.aws/amazonlinux/amazonlinux:2 \
		bash -c 'yum install -y golang zip && \
		         go build -o ./build/bootstrap . && \
				 cd build && \
		         zip function.zip bootstrap'; \
	done

kotlin:
	echo "Building gradle"
	./gradlew build --parallel

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

GO_LAMBDA_DIRS := authorizer

.PHONY: all go kotlin clean build

all: go kotlin

go:
	@for dir in $(GO_LAMBDA_DIRS); do \
		echo "Building $$dir..."; \
		cd golang/$$dir && GOOS=linux GOARCH=amd64 go build -o ./build/bootstrap . && cd build && zip function.zip bootstrap && cd ../../..; \
	done

kotlin:
	echo "Building gradle"
	./gradlew build

clean:
	echo "Cleaning kotlin"
	./gradlew clean

	echo "Cleaning go"
	@for dir in $(GO_LAMBDA_DIRS); do \
		echo "Cleaning $$dir..."; \
		rm -rf ./golang/$$dir/build; \
	done

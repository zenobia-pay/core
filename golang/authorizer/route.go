package main

import (
	"fmt"
	"strings"
)

type Route struct {
	Method string
	Path   string
}

func isValidPath(path string, routesToCheck []Route) bool {
	for _, v := range routesToCheck {
		if path == "/"+v.Path {
			return true
		}
	}
	return false
}

func generateOperationArns(methodArn string, routes []Route) ([]string, error) {
	var arns []string
	for _, v := range routes {
		arn, err := getOperationArn(methodArn, v)
		if err != nil {
			return nil, err
		}
		arns = append(arns, *arn)
	}
	return arns, nil
}

func getOperationArn(methodArn string, route Route) (*string, error) {
	parts := strings.Split(methodArn, "/")

	if len(parts) < 4 {
		return nil, fmt.Errorf("Invalid methodArn format %s", methodArn)
	}

	arn := fmt.Sprintf("%s/%s/%s", strings.Join(parts[:2], "/"), route.Method, route.Path)
	println("Got generated arn:" + arn)
	return &arn, nil
}

var validUnauthenticatedRoutes = []Route{
	{"POST", "create-link-token"},
	{"POST", "exchange-token"},
	{"POST", "issue-jwt"},
}

// TODO: use valid merchant and customer routes
var validMerchantRoutes = []Route{
	{"POST", "create-transfer-request"},
	{"GET", "get-merchant-transfer"},
	{"POST", "list-merchant-transfers"},
	{"POST", "list-merchant-payouts"},
	{"GET", "get-merchant-config"},
	{"POST", "update-merchant-config"},
	{"GET", "get-user-profile"},
	{"GET", "create-m2m-credentials"},
	{"POST", "list-m2m-credentials"},
	{"POST", "delete-m2m-credentials"},
	{"POST", "delete-bank-account"},
	{"POST", "list-bank-accounts"},
	{"POST", "submit-terms"},
}
var validCustomerRoutes = []Route{
	{"POST", "create-link-token"},
	{"POST", "exchange-token"},
	{"POST", "list-bank-accounts"},
	{"POST", "delete-bank-account"},
	{"POST", "fulfill-transfer"},
	{"GET", "get-customer-transfer"},
	{"POST", "list-customer-transfers"},
	{"GET", "get-user-profile"},
	{"POST", "submit-terms"},
	{"POST", "delete-refresh-token"},
}

var validOrumRoutes = []Route{
	{"POST", "orum-webhook"},
}

var validPlaidRoutes = []Route{
	{"POST", "plaid-webhook"},
}

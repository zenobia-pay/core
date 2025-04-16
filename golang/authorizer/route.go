package main

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

var validUnauthenticatedRoutes = []Route{
	{"POST", "create-link-token"},
	{"POST", "exchange-token"},
}

// TODO: use valid merchant and customer routes
var validMerchantRoutes = []Route{}
var validCustomerRoutes = []Route{}

var validOrumRoutes = []Route{
	{"POST", "orum-webhook"},
}

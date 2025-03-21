terraform {
  required_providers {
    auth0 = {
      source  = "auth0/auth0"
      version = ">= 1.0"
    }
  }
}

provider "auth0" {
  domain        = var.auth0_domain
  client_id     = var.auth0_client_id
  client_secret = var.auth0_client_secret
}

resource "auth0_client" "zenobia_app" {
  name            = "Zenobia Web Client Sandbox"
  app_type        = "spa"
  callbacks       = ["https://zenobiapay.com/callback", "http://localhost:5173/admin",  "http://localhost:5173", "http://localhost:5173/login"]
  allowed_logout_urls = ["https://zenobiapay.com/logout", "http://localhost:5173/admin"]
}

resource "auth0_client" "auth0_action_app" {
  name            = "auth0-action-app"
  description     = "Used by Auth0 Actions to call API Gateway"
  app_type        = "non_interactive"
  grant_types     = ["client_credentials"]
  is_first_party  = true
}

resource "auth0_resource_server" "zenobia_api" {
  name                 = "Zenobia API Sandbox"
  identifier           = "https://zenobiapay.com"
  signing_alg          = "RS256"
  token_lifetime       = 36000
}

output "client_id" {
  value = auth0_client.zenobia_app.client_id
}

output "api_identifier" {
  value = auth0_resource_server.zenobia_api.identifier
}
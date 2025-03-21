terraform {
  required_providers {
    auth0 = {
      source  = "auth0/auth0"
      version = "~> 1.0"
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

resource "auth0_action" "user_login_webhook" {
  name = "User-Login-Webhook"
  runtime = "node22"
  deploy = true
  supported_triggers {
    id      = "post-login"
    version = "v3"
  }
  secrets {
    name = "AUTH0_DOMAIN"
    value = var.auth0_domain
  }
  secrets {
    name = "CLIENT_ID"
    value = var.auth0_action_client_id
  }
  secrets {
    name = "CLIENT_SECRET"
    value = var.auth0_action_client_secret
  }
  secrets {
    name = "ZENOBIA_ENDPOINT"
    value = var.zenobia_endpoint
  }
  dependencies {
    name = "axios"
    version = "latest"
  }
  code = file("${path.module}/auth0/actions/post-login.js")
}

resource "auth0_trigger_actions" "bind_post_user_registration" {
  trigger = "post-login"

  actions {
    id           = auth0_action.user_login_webhook.id
    display_name = auth0_action.user_login_webhook.name
  }
}

output "client_id" {
  value = auth0_client.zenobia_app.client_id
}

output "api_identifier" {
  value = auth0_resource_server.zenobia_api.identifier
}
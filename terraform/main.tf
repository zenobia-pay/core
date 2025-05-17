terraform {
  required_providers {
    auth0 = {
      source  = "auth0/auth0"
      version = "~> 1.0"
    }
  }
  backend "s3" {
    use_lockfile = true
  }
}

provider "auth0" {
  domain        = var.AUTH0_DOMAIN
  client_id     = var.AUTH0_CLIENT_ID
  client_secret = var.AUTH0_CLIENT_SECRET
}

resource "auth0_client" "zenobia_app" {
  name            = "Zenobia Pay"
  app_type        = "regular_web"
  logo_uri = "https://zenobiapay.com/android-chrome-192x192.png"
  callbacks       = ["https://dashboard.zenobiapay.com/callback", "http://localhost:3000/admin",  "http://localhost:3000", "http://localhost:3000/login", "https://dashboard.zenobiapay.com/login", "zenobia://login-callback"]
  allowed_logout_urls = ["https://dashboard.zenobiapay.com", "http://localhost:3000"]
}

resource "auth0_client_grant" "zenobia_app_client_grant" {
  client_id = auth0_client.zenobia_app.id
  audience  = "https://dashboard.zenobiapay.com"
  scopes    = []
}

resource "auth0_client_credentials" "zenobia_app_credentials" {
  client_id = auth0_client.zenobia_app.id
  authentication_method = "none"
}

resource "auth0_client" "aws_auth0_management_app" {
  name            = "Auth0 M2M Management App"
  description     = "Used by Zenobia AWS service to manage client credentials for merchants"
  app_type        = "non_interactive"
  grant_types     = ["client_credentials"]
  is_first_party  = true
}

resource "auth0_client_grant" "aws_auth0_management_client_grant" {
  client_id = auth0_client.aws_auth0_management_app.id
  audience  = "https://${var.AUTH0_DOMAIN}/api/v2/"
  scopes    = ["create:clients", "update:clients", "delete:clients", "update:users_app_metadata", "read:users", "create:client_grants"]
}

resource "auth0_resource_server" "zenobia_api" {
  name                 = "Zenobia API"
  identifier           = "https://dashboard.zenobiapay.com"
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
  code = file("${path.module}/auth0/actions/post-login.js")
}

resource "auth0_trigger_actions" "bind_post_user_registration" {
  trigger = "post-login"

  actions {
    id           = auth0_action.user_login_webhook.id
    display_name = auth0_action.user_login_webhook.name
  }
}

resource "auth0_action" "credentials_exchange_webhook" {
  name = "Credentials-Exchange-Webhook"
  runtime = "node22"
  deploy = true
  supported_triggers {
    id      = "credentials-exchange"
    version = "v2"
  }
  code = file("${path.module}/auth0/actions/credentials-exchange.js")
}

resource "auth0_trigger_actions" "bind_credentials_exchange_registration" {
  trigger = "credentials-exchange"

  actions {
    id           = auth0_action.credentials_exchange_webhook.id
    display_name = auth0_action.credentials_exchange_webhook.name
  }
}

output "client_id" {
  value = auth0_client.zenobia_app.client_id
}

output "api_identifier" {
  value = auth0_resource_server.zenobia_api.identifier
}

resource "auth0_connection" "google_oauth2" {
  count    = var.ENVIRONMENT == "prod" ? 1 : 0
  name     = "google-oauth2"
  strategy = "google-oauth2"
  options {
    client_id     = var.GOOGLE_CLIENT_ID
    client_secret = var.GOOGLE_CLIENT_SECRET
    allowed_audiences = ["https://dashboard.zenobiapay.com"]
    scopes = ["email", "profile"]
  }
}

resource "auth0_connection_clients" "google_oauth2_clients" {
  count           = var.ENVIRONMENT == "prod" ? 1 : 0
  enabled_clients = [auth0_client.zenobia_app.id]
  connection_id   = auth0_connection.google_oauth2[0].id
}

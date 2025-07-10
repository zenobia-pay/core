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
  callbacks       = var.ENVIRONMENT == "prod" ? [
    "https://dashboard.zenobiapay.com/callback",
    "https://dashboard.zenobiapay.com/login",
    "zenobia://login-callback",
    "http://localhost:3000",
    "http://localhost:3000/login"
  ] : [
    "https://beta-dashboard.zenobiapay.com/callback",
    "https://beta-dashboard.zenobiapay.com/login",
    "zenobia://login-callback",
    "http://localhost:3000",
    "http://localhost:3000/login"
  ]
  allowed_logout_urls = var.ENVIRONMENT == "prod" ? [
    "https://dashboard.zenobiapay.com",
    "http://localhost:3000"
  ] : [
    "https://beta-dashboard.zenobiapay.com",
    "http://localhost:3000"
  ]
  # Disable Username-Password Authentication
  is_first_party = true
  oidc_conformant = true
  jwt_configuration {
    alg = "RS256"
  }
  # Restrict to social connections only (Google OAuth)
  client_metadata = {
    disable_username_password_authentication = "true"
  }
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

# New Auth0 client for admin interface
resource "auth0_client" "zenobia_admin_app" {
  name            = "Zenobia Admin"
  app_type        = "regular_web"
  logo_uri        = "https://zenobiapay.com/android-chrome-192x192.png"
  callbacks       = var.ENVIRONMENT == "prod" ? [
    "https://admin.zenobiapay.com/callback",
    "https://admin.zenobiapay.com/login",
    "http://localhost:3001/callback",
    "http://localhost:3001/login"
  ] : [
    "https://beta-admin.zenobiapay.com/callback",
    "https://beta-admin.zenobiapay.com/login",
    "http://localhost:3001/callback",
    "http://localhost:3001/login"
  ]
  allowed_logout_urls = var.ENVIRONMENT == "prod" ? [
    "https://admin.zenobiapay.com",
    "http://localhost:3001"
  ] : [
    "https://beta-admin.zenobiapay.com",
    "http://localhost:3001"
  ]
  jwt_configuration {
    alg = "RS256"
  }
  # Add metadata to identify this as an admin application
  client_metadata = {
    role = "ADMIN"
  }
  # Enable required grant types for refresh tokens
  grant_types = [
    "authorization_code",
    "implicit",
    "refresh_token"
  ]
  # Enable MFA (2FA) for this application
  initiate_login_uri = var.ENVIRONMENT == "prod" ? "https://admin.zenobiapay.com/login" : "https://beta-admin.zenobiapay.com/login"
  refresh_token {
    rotation_type   = "rotating"
    expiration_type = "expiring"
    leeway          = 0
    token_lifetime  = 2592000 # 30 days
    idle_token_lifetime = 1296000 # 15 days
    infinite_token_lifetime = false
    infinite_idle_token_lifetime = false
  }
  # Require MFA
  oidc_conformant = true
}

resource "auth0_client_grant" "zenobia_admin_app_client_grant" {
  client_id = auth0_client.zenobia_admin_app.id
  audience  = "https://admin.zenobiapay.com"
  scopes    = []
}

resource "auth0_client_credentials" "zenobia_admin_app_credentials" {
  client_id = auth0_client.zenobia_admin_app.id
  authentication_method = "none"
}

resource "auth0_resource_server" "zenobia_admin_api" {
  name                 = "Zenobia Admin API"
  identifier           = "https://admin.zenobiapay.com"
  signing_alg          = "RS256"
  token_lifetime       = 36000
  skip_consent_for_verifiable_first_party_clients = true
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

# Define Auth0 roles
resource "auth0_role" "merchant_role" {
  name        = "Merchant"
  description = "Regular merchant user with standard permissions"
}

resource "auth0_role" "admin_role" {
  name        = "Admin"
  description = "Administrator with elevated permissions"
}

output "client_id" {
  value = auth0_client.zenobia_app.client_id
}

output "api_identifier" {
  value = auth0_resource_server.zenobia_api.identifier
}

output "admin_client_id" {
  value = auth0_client.zenobia_admin_app.client_id
}

output "admin_api_identifier" {
  value = auth0_resource_server.zenobia_admin_api.identifier
}

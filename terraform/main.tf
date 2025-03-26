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
  app_type        = "regular_web"
  logo_uri = "https://zenobiapay.com/android-chrome-192x192.png"
  callbacks       = ["https://zenobiapay.com/callback", "http://localhost:3000/admin",  "http://localhost:3000", "http://localhost:3000/login", "https://zenobiapay.com/login", "http:zenobiapay.com/admin"]
  allowed_logout_urls = ["https://zenobiapay.com/logout", "http://localhost:3000/admin"]
}

resource "auth0_client_credentials" "zenobia_app_credentials" {
  client_id = auth0_client.zenobia_app.id
  authentication_method = "none"
}

resource "auth0_client" "zenobia_merchant_app" {
  name            = "Zenobia Merchant Client"
  app_type        = "regular_web"
  logo_uri = "https://zenobiapay.com/android-chrome-192x192.png"
  callbacks       = ["https://zenobiapay.com/callback", "http://localhost:3000/admin",  "http://localhost:3000", "http://localhost:3000/login"]
  allowed_logout_urls = ["https://zenobiapay.com/logout", "http://localhost:3000/admin", "http://localhost:3000"]
}

resource "auth0_client_credentials" "zenobia_merchant_credentials" {
  client_id = auth0_client.zenobia_merchant_app.id
  authentication_method = "none"
}

resource "auth0_role" "merchant" {
  name        = "merchant"
  description = "Merchant role that allows requesting transfers"
}

resource "auth0_role" "customer" {
  name        = "customer"
  description = "Standard customer role that can authorize pushes"
}

resource "auth0_client" "auth0_action_app" {
  name            = "auth0-action-app"
  description     = "Used by Auth0 Actions to call API Gateway"
  app_type        = "non_interactive"
  grant_types     = ["client_credentials"]
  is_first_party  = true
}

resource "auth0_client" "aws_auth0_management_app" {
  name            = "aws-auth0-management-app"
  description     = "Used by Zenobia AWS service to manage client credentials for merchants"
  app_type        = "non_interactive"
  grant_types     = ["client_credentials"]
  is_first_party  = true
}

resource "auth0_client_grant" "aws_auth0_management_client_grant" {
  client_id = auth0_client.aws_auth0_management_app.id
  audience  = "https://dev-u0ert1rxhkdmhwy8.us.auth0.com/api/v2/"
  scopes    = ["create:clients", "update:clients", "delete:clients"]
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
    value = auth0_client.auth0_action_app.client_id
  }
  secrets {
    name = "CLIENT_SECRET"
    value = var.auth0_action_client_secret
  }
  secrets {
    name = "AUTH0_CLIENT_ID"
    value = var.auth0_client_id
  }
  secrets {
    name = "AUTH0_CLIENT_SECRET"
    value = var.auth0_client_secret
  }
  secrets {
    name = "ZENOBIA_ENDPOINT"
    value = var.zenobia_endpoint
  }
  secrets {
    name = "MERCHANT_CLIENT_ID"
    value = auth0_client.zenobia_merchant_app.client_id
  }
  secrets {
    name = "CUSTOMER_CLIENT_ID"
    value = auth0_client.zenobia_app.client_id
  }
  secrets {
    name = "MERCHANT_ROLE_ID"
    value = auth0_role.merchant.id
  }
  secrets {
    name = "CUSTOMER_ROLE_ID"
    value = auth0_role.customer.id
  }
  dependencies {
    name = "axios"
    version = "latest"
  }
  dependencies {
    name = "auth0"
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

output "merchant_client_id" {
  value = auth0_client.zenobia_merchant_app.client_id
}

output "api_identifier" {
  value = auth0_resource_server.zenobia_api.identifier
}

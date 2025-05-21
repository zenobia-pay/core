variable "AUTH0_DOMAIN" {
  description = "Auth0 domain"
  type        = string
}

variable "AUTH0_CLIENT_ID" {
  description = "Terraform provider Auth0 client ID"
  type        = string
}

variable "AUTH0_CLIENT_SECRET" {
  description = "Terraform provider Auth0 client secret"
  type        = string
  sensitive   = true
}

variable "GOOGLE_CLIENT_ID" {
  description = "Google OAuth client ID"
  type        = string
  default     = ""  # Empty default for non-prod environments
}

variable "GOOGLE_CLIENT_SECRET" {
  description = "Google OAuth client secret"
  type        = string
  sensitive   = true
  default     = ""  # Empty default for non-prod environments
}

variable "ENVIRONMENT" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}
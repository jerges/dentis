# ─── Azure Identity ──────────────────────────────────────────────────────────
variable "azure_subscription_id" {
  description = "Azure Subscription ID — set via ARM_SUBSCRIPTION_ID env var (recommended)"
  type        = string
  default     = ""
}

variable "azure_tenant_id" {
  description = "Azure Tenant ID — set via ARM_TENANT_ID env var (recommended)"
  type        = string
  default     = ""
}

variable "azure_client_id" {
  description = "Service Principal Client ID — set via ARM_CLIENT_ID env var"
  type        = string
  sensitive   = true
  default     = ""
}

variable "azure_client_secret" {
  description = "Service Principal Client Secret — set via ARM_CLIENT_SECRET env var"
  type        = string
  sensitive   = true
  default     = ""
}

# ─── General ─────────────────────────────────────────────────────────────────
variable "location" {
  description = "Azure region to deploy resources"
  type        = string
  default     = "East US"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "prod"

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod"
  }
}

variable "app_name" {
  description = "Application name used for naming Azure resources"
  type        = string
  default     = "dentis"
}

# ─── Networking ──────────────────────────────────────────────────────────────
variable "vnet_address_space" {
  description = "Address space for the Virtual Network"
  type        = list(string)
  default     = ["10.0.0.0/16"]
}

variable "app_subnet_cidr" {
  description = "CIDR for the Container Apps subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "db_subnet_cidr" {
  description = "CIDR for the Database subnet"
  type        = string
  default     = "10.0.2.0/24"
}

# ─── Application ─────────────────────────────────────────────────────────────
variable "app_port" {
  description = "Port the Spring Boot application listens on"
  type        = number
  default     = 8080
}

variable "app_cpu" {
  description = "CPU cores allocated to the container (0.25, 0.5, 1, 2)"
  type        = number
  default     = 0.5
}

variable "app_memory" {
  description = "Memory in GB (must match CPU tier: 0.5→1Gi, 1→2Gi, 2→4Gi)"
  type        = string
  default     = "1Gi"
}

variable "app_min_replicas" {
  description = "Minimum number of container replicas (0 for scale-to-zero)"
  type        = number
  default     = 1
}

variable "app_max_replicas" {
  description = "Maximum number of container replicas"
  type        = number
  default     = 3
}

variable "app_image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

# ─── Database (Azure PostgreSQL Flexible Server) ──────────────────────────────
variable "db_sku" {
  description = "PostgreSQL Flexible Server SKU"
  type        = string
  default     = "B_Standard_B1ms"
}

variable "db_storage_mb" {
  description = "PostgreSQL storage size in MB"
  type        = number
  default     = 32768
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "dentis_db"
}

variable "db_admin_login" {
  description = "PostgreSQL administrator login"
  type        = string
  default     = "dentisadmin"
  sensitive   = true
}

variable "db_admin_password" {
  description = "PostgreSQL administrator password — set via TF_VAR_db_admin_password"
  type        = string
  sensitive   = true
}

variable "db_version" {
  description = "PostgreSQL version"
  type        = string
  default     = "16"
}

# ─── Application Secrets ─────────────────────────────────────────────────────
variable "jwt_secret" {
  description = "JWT signing secret (min 32 chars) — set via TF_VAR_jwt_secret"
  type        = string
  sensitive   = true
}

variable "mail_host" {
  description = "SMTP server hostname"
  type        = string
  default     = "smtp.gmail.com"
}

variable "mail_port" {
  description = "SMTP server port"
  type        = number
  default     = 587
}

variable "mail_username" {
  description = "SMTP username — set via TF_VAR_mail_username"
  type        = string
  sensitive   = true
  default     = ""
}

variable "mail_password" {
  description = "SMTP password — set via TF_VAR_mail_password"
  type        = string
  sensitive   = true
  default     = ""
}

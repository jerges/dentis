data "azurerm_client_config" "current" {}

locals {
  name_prefix  = "${var.app_name}-${var.environment}"
  short_prefix = "${var.app_name}${var.environment}"
}

# ─── Resource Group ───────────────────────────────────────────────────────────
resource "azurerm_resource_group" "main" {
  name     = "rg-${local.name_prefix}"
  location = var.location

  tags = {
    Project     = var.app_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

# ─── Networking ───────────────────────────────────────────────────────────────
module "networking" {
  source = "./modules/networking"

  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  name_prefix         = local.name_prefix
  vnet_address_space  = var.vnet_address_space
  app_subnet_cidr     = var.app_subnet_cidr
  db_subnet_cidr      = var.db_subnet_cidr
}

# ─── Azure Container Registry ─────────────────────────────────────────────────
module "acr" {
  source = "./modules/acr"

  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  name_prefix         = local.short_prefix
}

# ─── Key Vault ────────────────────────────────────────────────────────────────
module "keyvault" {
  source = "./modules/keyvault"

  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  name_prefix         = local.name_prefix
  tenant_id           = data.azurerm_client_config.current.tenant_id
  current_object_id   = data.azurerm_client_config.current.object_id

  secrets = {
    db-admin-password = var.db_admin_password
    jwt-secret        = var.jwt_secret
    mail-username     = var.mail_username
    mail-password     = var.mail_password
  }
}

# ─── PostgreSQL Flexible Server ───────────────────────────────────────────────
module "database" {
  source = "./modules/database"

  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  name_prefix         = local.name_prefix
  db_subnet_id        = module.networking.db_subnet_id
  private_dns_zone_id = module.networking.private_dns_zone_id
  db_sku              = var.db_sku
  db_storage_mb       = var.db_storage_mb
  db_name             = var.db_name
  db_admin_login      = var.db_admin_login
  db_admin_password   = var.db_admin_password
  db_version          = var.db_version
}

# ─── Container Apps ───────────────────────────────────────────────────────────
module "container_app" {
  source = "./modules/container-app"

  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  name_prefix         = local.name_prefix
  app_subnet_id       = module.networking.app_subnet_id
  acr_login_server    = module.acr.login_server
  acr_admin_username  = module.acr.admin_username
  acr_admin_password  = module.acr.admin_password
  image_tag           = var.app_image_tag
  app_port            = var.app_port
  cpu                 = var.app_cpu
  memory              = var.app_memory
  min_replicas        = var.app_min_replicas
  max_replicas        = var.app_max_replicas

  environment_variables = [
    { name = "SPRING_PROFILES_ACTIVE", value = var.environment,         secret_ref = "" },
    { name = "DB_HOST",                value = module.database.fqdn,     secret_ref = "" },
    { name = "DB_PORT",                value = "5432",                   secret_ref = "" },
    { name = "DB_NAME",                value = var.db_name,              secret_ref = "" },
    { name = "DB_USER",                value = var.db_admin_login,       secret_ref = "" },
    { name = "MAIL_HOST",              value = var.mail_host,            secret_ref = "" },
    { name = "MAIL_PORT",              value = tostring(var.mail_port),  secret_ref = "" },
    { name = "JWT_EXPIRATION_MS",      value = "86400000",               secret_ref = "" },
    { name = "SERVER_PORT",            value = tostring(var.app_port),   secret_ref = "" },
  ]

  secrets = [
    { name = "db-password",   value = var.db_admin_password },
    { name = "jwt-secret",    value = var.jwt_secret },
    { name = "mail-username", value = var.mail_username },
    { name = "mail-password", value = var.mail_password },
  ]

  secret_env_refs = [
    { name = "DB_PASSWORD",   secret_ref = "db-password" },
    { name = "JWT_SECRET",    secret_ref = "jwt-secret" },
    { name = "MAIL_USERNAME", secret_ref = "mail-username" },
    { name = "MAIL_PASSWORD", secret_ref = "mail-password" },
  ]
}

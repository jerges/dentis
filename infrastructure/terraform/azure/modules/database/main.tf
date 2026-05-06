resource "azurerm_postgresql_flexible_server" "main" {
  name                   = "psql-${var.name_prefix}"
  resource_group_name    = var.resource_group_name
  location               = var.location
  version                = var.db_version
  delegated_subnet_id    = var.db_subnet_id
  private_dns_zone_id    = var.private_dns_zone_id
  administrator_login    = var.db_admin_login
  administrator_password = var.db_admin_password
  zone                   = "1"
  storage_mb             = var.db_storage_mb

  sku_name = var.db_sku

  backup_retention_days        = 7
  geo_redundant_backup_enabled = false

  tags = { Name = "psql-${var.name_prefix}" }
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = var.db_name
  server_id = azurerm_postgresql_flexible_server.main.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

resource "azurerm_postgresql_flexible_server_configuration" "log_connections" {
  name      = "log_connections"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "extensions" {
  name      = "azure.extensions"
  server_id = azurerm_postgresql_flexible_server.main.id
  value     = "UUID-OSSP"
}

resource "azurerm_key_vault" "main" {
  name                        = "kv-${var.name_prefix}"
  resource_group_name         = var.resource_group_name
  location                    = var.location
  tenant_id                   = var.tenant_id
  sku_name                    = "standard"
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false
  enable_rbac_authorization   = false

  access_policy {
    tenant_id = var.tenant_id
    object_id = var.current_object_id

    secret_permissions = ["Get", "List", "Set", "Delete", "Purge"]
  }
}

resource "azurerm_key_vault_secret" "app_secrets" {
  for_each     = var.secrets
  name         = each.key
  value        = each.value
  key_vault_id = azurerm_key_vault.main.id

  lifecycle {
    ignore_changes = [value]
  }
}

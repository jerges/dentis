output "vnet_id"              { value = azurerm_virtual_network.main.id }
output "app_subnet_id"        { value = azurerm_subnet.app.id }
output "db_subnet_id"         { value = azurerm_subnet.db.id }
output "private_dns_zone_id"  { value = azurerm_private_dns_zone.postgres.id }

output "fqdn"       { value = "https://${azurerm_container_app.app.ingress[0].fqdn}" }
output "app_name"   { value = azurerm_container_app.app.name }
output "env_name"   { value = azurerm_container_app_environment.main.name }

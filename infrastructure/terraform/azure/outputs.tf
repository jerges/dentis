output "app_url" {
  description = "Public URL of the Container App"
  value       = module.container_app.fqdn
}

output "acr_login_server" {
  description = "Azure Container Registry login server"
  value       = module.acr.login_server
}

output "db_fqdn" {
  description = "PostgreSQL Flexible Server FQDN"
  value       = module.database.fqdn
  sensitive   = true
}

output "resource_group_name" {
  description = "Resource group name"
  value       = azurerm_resource_group.main.name
}

output "keyvault_uri" {
  description = "Key Vault URI"
  value       = module.keyvault.vault_uri
}

output "docker_push_commands" {
  description = "Commands to build and push the Docker image to ACR"
  value       = <<-EOT
    # Authenticate Docker with ACR
    az acr login --name ${module.acr.login_server}

    # Build from project root
    docker build -f infrastructure/docker/Dockerfile \
      -t ${module.acr.login_server}/${var.app_name}:latest .

    # Push
    docker push ${module.acr.login_server}/${var.app_name}:latest

    # Update Container App image
    az containerapp update \
      --name ${local.name_prefix}-app \
      --resource-group ${azurerm_resource_group.main.name} \
      --image ${module.acr.login_server}/${var.app_name}:latest
  EOT
}

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

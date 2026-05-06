resource "azurerm_log_analytics_workspace" "main" {
  name                = "law-${var.name_prefix}"
  resource_group_name = var.resource_group_name
  location            = var.location
  sku                 = "PerGB2018"
  retention_in_days   = 30
}

resource "azurerm_container_app_environment" "main" {
  name                       = "cae-${var.name_prefix}"
  resource_group_name        = var.resource_group_name
  location                   = var.location
  log_analytics_workspace_id = azurerm_log_analytics_workspace.main.id
  infrastructure_subnet_id   = var.app_subnet_id
}

resource "azurerm_container_app" "app" {
  name                         = "${var.name_prefix}-app"
  resource_group_name          = var.resource_group_name
  container_app_environment_id = azurerm_container_app_environment.main.id
  revision_mode                = "Single"

  registry {
    server               = var.acr_login_server
    username             = var.acr_admin_username
    password_secret_name = "acr-password"
  }

  dynamic "secret" {
    for_each = concat(
      [{ name = "acr-password", value = var.acr_admin_password }],
      var.secrets
    )
    content {
      name  = secret.value.name
      value = secret.value.value
    }
  }

  template {
    min_replicas = var.min_replicas
    max_replicas = var.max_replicas

    container {
      name   = "dentis-api"
      image  = "${var.acr_login_server}/dentis:${var.image_tag}"
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = var.environment_variables
        content {
          name  = env.value.name
          value = env.value.secret_ref == "" ? env.value.value : null
        }
      }

      dynamic "env" {
        for_each = var.secret_env_refs
        content {
          name        = env.value.name
          secret_name = env.value.secret_ref
        }
      }

      liveness_probe {
        transport = "HTTP"
        port      = var.app_port
        path      = "/actuator/health"
        initial_delay    = 60
        interval_seconds = 30
      }

      readiness_probe {
        transport = "HTTP"
        port      = var.app_port
        path      = "/actuator/health"
        interval_seconds = 10
      }
    }
  }

  ingress {
    external_enabled = true
    target_port      = var.app_port
    traffic_weight {
      percentage      = 100
      latest_revision = true
    }
  }
}

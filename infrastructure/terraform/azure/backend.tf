terraform {
  required_version = ">= 1.7.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.110"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 2.53"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Remote state en Azure Blob Storage — completar con tu storage account
  backend "azurerm" {
    resource_group_name  = "REPLACE_WITH_TFSTATE_RESOURCE_GROUP"
    storage_account_name = "REPLACE_WITH_STORAGE_ACCOUNT_NAME"
    container_name       = "tfstate"
    key                  = "dentis/terraform.tfstate"
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = false
    }
    resource_group {
      prevent_deletion_if_contains_resources = false
    }
  }

  # Autenticación — completar con tu Service Principal o usar az login
  # subscription_id = var.azure_subscription_id
  # client_id       = var.azure_client_id
  # client_secret   = var.azure_client_secret
  # tenant_id       = var.azure_tenant_id
}

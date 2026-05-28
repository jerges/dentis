terraform {
  required_version = ">= 1.7.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }

  # Remote state en S3 — completar con tu bucket y region
  backend "s3" {
    bucket         = "REPLACE_WITH_YOUR_TERRAFORM_STATE_BUCKET"
    key            = "dentis/terraform.tfstate"
    region         = "REPLACE_WITH_YOUR_AWS_REGION"
    encrypt        = true
    dynamodb_table = "REPLACE_WITH_YOUR_DYNAMODB_LOCK_TABLE"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "dentis"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

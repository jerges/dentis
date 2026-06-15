# ─── AWS ────────────────────────────────────────────────────────────────────
variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
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

# ─── Networking ─────────────────────────────────────────────────────────────
variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones to use"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (ALB)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (ECS + RDS)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

# ─── Application ────────────────────────────────────────────────────────────
variable "app_name" {
  description = "Application name used for naming resources"
  type        = string
  default     = "dentis"
}

variable "app_port" {
  description = "Port the Spring Boot application listens on"
  type        = number
  default     = 8080
}

variable "app_cpu" {
  description = "ECS task CPU units (256, 512, 1024, 2048, 4096)"
  type        = number
  default     = 512
}

variable "app_memory" {
  description = "ECS task memory in MB"
  type        = number
  default     = 1024
}

variable "app_desired_count" {
  description = "Number of ECS task replicas"
  type        = number
  default     = 1
}

variable "app_image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

# ─── Database ────────────────────────────────────────────────────────────────
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "dentis_db"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "dentis"
  sensitive   = true
}

variable "db_password" {
  description = "PostgreSQL master password — set via TF_VAR_db_password env var, never hardcode"
  type        = string
  sensitive   = true
}

variable "db_allocated_storage" {
  description = "RDS storage in GB"
  type        = number
  default     = 20
}

variable "db_multi_az" {
  description = "Enable RDS Multi-AZ for high availability"
  type        = bool
  default     = false
}

# ─── Application Secrets ─────────────────────────────────────────────────────
variable "jwt_secret" {
  description = "JWT signing secret (min 256 bits) — set via TF_VAR_jwt_secret env var"
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
  description = "SMTP username — set via TF_VAR_mail_username env var"
  type        = string
  sensitive   = true
  default     = ""
}

variable "mail_password" {
  description = "SMTP password — set via TF_VAR_mail_password env var"
  type        = string
  sensitive   = true
  default     = ""
}

# ─── ACM / HTTPS ─────────────────────────────────────────────────────────────
variable "certificate_arn" {
  description = "ACM certificate ARN for HTTPS. Leave empty to use HTTP only (not recommended for prod)"
  type        = string
  default     = ""
}

variable "domain_name" {
  description = "Domain name for the application (optional, for Route53)"
  type        = string
  default     = ""
}

# ─── Landing page ─────────────────────────────────────────────────────────────
variable "landing_domain_name" {
  description = "Custom domain for the landing page (e.g. dentis.com.ve). Leave empty to use CloudFront URL."
  type        = string
  default     = ""
}

variable "landing_certificate_arn" {
  description = "ACM certificate ARN (us-east-1) for the landing domain. Required if landing_domain_name is set."
  type        = string
  default     = ""
}

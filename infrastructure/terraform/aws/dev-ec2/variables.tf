variable "aws_region" {
  description = "AWS region for the dev EC2 stack"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment label"
  type        = string
  default     = "dev"
}

variable "app_name" {
  description = "Application name used for resource naming"
  type        = string
  default     = "dentis"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t4g.small"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB"
  type        = number
  default     = 30
}

variable "ssh_allowed_cidr" {
  description = "CIDR block allowed to connect via SSH (recommended x.x.x.x/32)"
  type        = string
}

variable "app_allowed_cidr" {
  description = "CIDR block allowed to access the app HTTP port"
  type        = string
  default     = "0.0.0.0/0"
}

variable "web_allowed_cidr" {
  description = "CIDR block allowed to access the frontend HTTP port"
  type        = string
  default     = "0.0.0.0/0"
}

variable "app_port" {
  description = "Application HTTP port published on the host"
  type        = number
  default     = 8080
}

variable "web_port" {
  description = "Frontend (backoffice) HTTP port published on the host"
  type        = number
  default     = 8081
}

variable "landing_port" {
  description = "Landing page HTTP port published on the host"
  type        = number
  default     = 80
}


variable "assign_eip" {
  description = "Assign Elastic IP for stable public address across stop/start cycles"
  type        = bool
  default     = true
}

variable "create_key_pair" {
  description = "Create a key pair in AWS using ssh_public_key_path"
  type        = bool
  default     = true
}

variable "ssh_public_key_path" {
  description = "Path to local SSH public key (used only when create_key_pair=true)"
  type        = string
  default     = "~/.ssh/dentis-dev-ec2.pub"
}

variable "existing_key_pair_name" {
  description = "Existing EC2 key pair name (used only when create_key_pair=false)"
  type        = string
  default     = "mac-jb-key"
}

variable "ecr_repository_url" {
  description = "Private ECR repository URL without tag"
  type        = string
}

variable "app_image_tag" {
  description = "Docker image tag to deploy from ECR"
  type        = string
  default     = "latest"
}

variable "web_ecr_repository_url" {
  description = "Private ECR repository URL for the web image without tag"
  type        = string
}

variable "web_image_tag" {
  description = "Docker image tag to deploy for web"
  type        = string
  default     = "latest"
}

variable "db_name" {
  description = "Local PostgreSQL database name"
  type        = string
  default     = "dentis_db"
}

variable "db_user" {
  description = "Local PostgreSQL username"
  type        = string
  default     = "dentis"
}

variable "db_password" {
  description = "Local PostgreSQL password"
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret"
  type        = string
  sensitive   = true
}

variable "mail_host" {
  description = "SMTP host for the app"
  type        = string
  default     = "mailhog"
}

variable "mail_port" {
  description = "SMTP port for the app"
  type        = number
  default     = 1025
}

variable "mail_username" {
  description = "SMTP username"
  type        = string
  default     = ""
}

variable "mail_password" {
  description = "SMTP password"
  type        = string
  sensitive   = true
  default     = ""
}

variable "spring_profiles_active" {
  description = "Spring profile used by the app"
  type        = string
  default     = "dev"
}

variable "alert_email" {
  description = "Email address for CloudWatch alarm notifications (SNS). Leave empty to skip subscription."
  type        = string
  default     = ""
}

variable "vpc_cidr" {
  description = "CIDR for dedicated dev VPC"
  type        = string
  default     = "10.60.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR for the public subnet"
  type        = string
  default     = "10.60.1.0/24"
}

variable "availability_zone" {
  description = "Specific AZ for the subnet. Leave null to auto-pick first AZ"
  type        = string
  default     = null
}

variable "attachments_bucket_name" {
  description = "S3 bucket name for clinical attachments (images/X-rays). Leave empty to auto-generate."
  type        = string
  default     = ""
}

variable "prometheus_port" {
  description = "Prometheus HTTP port published on the host"
  type        = number
  default     = 9090
}

variable "grafana_port" {
  description = "Grafana HTTP port published on the host"
  type        = number
  default     = 3000
}

variable "monitoring_allowed_cidr" {
  description = "CIDR allowed to access Prometheus and Grafana (restrict to your IP in production)"
  type        = string
  default     = "0.0.0.0/0"
}

variable "auto_stop_max_runtime_hours" {
  description = "Hours the EC2 can be running before the auto-stop Lambda shuts it down"
  type        = number
  default     = 2
}


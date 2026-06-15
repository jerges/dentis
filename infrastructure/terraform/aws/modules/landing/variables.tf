variable "name_prefix" {
  description = "Prefix for all resource names"
  type        = string
  default     = "dentis"
}

variable "domain_name" {
  description = "Custom domain for the landing page (e.g. dentis.com.ve). Leave empty to use CloudFront URL."
  type        = string
  default     = ""
}

variable "certificate_arn" {
  description = "ACM certificate ARN (us-east-1) for the custom domain. Required if domain_name is set."
  type        = string
  default     = ""
}


variable "aws_region" {
  description = "AWS region for all resources"
  type        = string
  default     = "us-east-1"
}

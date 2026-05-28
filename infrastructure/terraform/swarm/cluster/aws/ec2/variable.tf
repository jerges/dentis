variable "my_access_key" {
    description = "Access-key-for-AWS"
    default     = "no_access_key_value_found"
}

variable "my_secret_key" {
    description = "Secret-key-for-AWS"
    default     = "no_secret_key_value_found"
}

variable "repository_url" {
    description = "ECR Repository URL"
    type        = string
}

variable "region" {
    description = "region to deploy"
    type        = string
}

variable "environment_tag" {
    description = "environment of service"
    type        = string
}

variable "certificate_private_url" {
    description = "Private certificate URL"
    type        = string
}

variable "certificate_public_url" {
    description = "Public certificate URL"
    type        = string
}

variable "certificate_name" {
    description = "Name certificate"
    type        = string
}

variable "certificate_arn" {
    description = "The ARN of the ACM certificate"
    type        = string
}





variable "certificate_private_url" {
    description = "Private certificate URL"
    type        = string
}

variable "docker_image" {
    description = "Docker image to install"
    type        = string
}

variable "docker_name" {
    description = "Docker name to install"
    type        = string
}

variable "public_dns" {
    description = "public dns to connect docker"
    type        = string
}
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

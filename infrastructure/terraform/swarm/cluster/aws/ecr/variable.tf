variable "repository_name" {
    description = "name repository image"
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

variable "region" {
    description = "region to deploy"
    type        = string
}

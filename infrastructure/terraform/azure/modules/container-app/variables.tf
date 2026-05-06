variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "name_prefix"         { type = string }
variable "app_subnet_id"       { type = string }
variable "acr_login_server"    { type = string }
variable "acr_admin_username"  { type = string; sensitive = true }
variable "acr_admin_password"  { type = string; sensitive = true }
variable "image_tag"           { type = string }
variable "app_port"            { type = number }
variable "cpu"                 { type = number }
variable "memory"              { type = string }
variable "min_replicas"        { type = number }
variable "max_replicas"        { type = number }

variable "environment_variables" {
  type = list(object({
    name       = string
    value      = string
    secret_ref = string
  }))
}

variable "secrets" {
  type      = list(object({ name = string, value = string }))
  sensitive = true
}

variable "secret_env_refs" {
  type = list(object({ name = string, secret_ref = string }))
}

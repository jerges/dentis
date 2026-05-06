variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "name_prefix"         { type = string }
variable "db_subnet_id"        { type = string }
variable "private_dns_zone_id" { type = string }
variable "db_sku"              { type = string }
variable "db_storage_mb"       { type = number }
variable "db_name"             { type = string }
variable "db_admin_login"      { type = string; sensitive = true }
variable "db_admin_password"   { type = string; sensitive = true }
variable "db_version"          { type = string }

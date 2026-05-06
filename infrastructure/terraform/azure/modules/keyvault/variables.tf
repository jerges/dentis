variable "resource_group_name" { type = string }
variable "location"            { type = string }
variable "name_prefix"         { type = string }
variable "tenant_id"           { type = string }
variable "current_object_id"   { type = string }
variable "secrets"             { type = map(string); sensitive = true }

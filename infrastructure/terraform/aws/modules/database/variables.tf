variable "name_prefix"        { type = string }
variable "vpc_id"             { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "db_sg_id"           { type = string }
variable "db_instance_class"  { type = string }
variable "db_name"            { type = string }
variable "db_username"        { type = string; sensitive = true }
variable "db_password"        { type = string; sensitive = true }
variable "allocated_storage"  { type = number }
variable "multi_az"           { type = bool }

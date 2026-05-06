variable "name_prefix"   { type = string }
variable "db_username"   { type = string; sensitive = true; default = "dentis" }
variable "db_password"   { type = string; sensitive = true }
variable "jwt_secret"    { type = string; sensitive = true }
variable "mail_username" { type = string; sensitive = true; default = "" }
variable "mail_password" { type = string; sensitive = true; default = "" }

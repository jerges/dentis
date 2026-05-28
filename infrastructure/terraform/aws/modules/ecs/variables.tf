variable "name_prefix"        { type = string }
variable "app_name"           { type = string }
variable "vpc_id"             { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "public_subnet_ids"  { type = list(string) }
variable "alb_sg_id"          { type = string }
variable "ecs_sg_id"          { type = string }
variable "app_port"           { type = number }
variable "cpu"                { type = number }
variable "memory"             { type = number }
variable "desired_count"      { type = number }
variable "image_uri"          { type = string }
variable "certificate_arn"    { type = string }
variable "secrets_arn"        { type = string }

variable "environment_variables" {
  type = list(object({ name = string, value = string }))
}

variable "secrets" {
  type = list(object({ name = string, valueFrom = string }))
}

locals {
  name_prefix = "${var.app_name}-${var.environment}"
}

# ─── Networking ──────────────────────────────────────────────────────────────
module "networking" {
  source = "./modules/networking"

  name_prefix          = local.name_prefix
  vpc_cidr             = var.vpc_cidr
  availability_zones   = var.availability_zones
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  app_port             = var.app_port
}

# ─── ECR Repository ──────────────────────────────────────────────────────────
module "ecr" {
  source = "./modules/ecr"

  name_prefix = local.name_prefix
  app_name    = var.app_name
}

# ─── Secrets Manager ─────────────────────────────────────────────────────────
module "secrets" {
  source = "./modules/secrets"

  name_prefix   = local.name_prefix
  db_password   = var.db_password
  jwt_secret    = var.jwt_secret
  mail_username = var.mail_username
  mail_password = var.mail_password
}

# ─── RDS PostgreSQL ──────────────────────────────────────────────────────────
module "database" {
  source = "./modules/database"

  name_prefix        = local.name_prefix
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  db_sg_id           = module.networking.db_sg_id
  db_instance_class  = var.db_instance_class
  db_name            = var.db_name
  db_username        = var.db_username
  db_password        = var.db_password
  allocated_storage  = var.db_allocated_storage
  multi_az           = var.db_multi_az
}

# ─── ECS Fargate ─────────────────────────────────────────────────────────────
module "ecs" {
  source = "./modules/ecs"

  name_prefix        = local.name_prefix
  app_name           = var.app_name
  vpc_id             = module.networking.vpc_id
  private_subnet_ids = module.networking.private_subnet_ids
  public_subnet_ids  = module.networking.public_subnet_ids
  alb_sg_id          = module.networking.alb_sg_id
  ecs_sg_id          = module.networking.ecs_sg_id
  app_port           = var.app_port
  cpu                = var.app_cpu
  memory             = var.app_memory
  desired_count      = var.app_desired_count
  certificate_arn    = var.certificate_arn

  image_uri = "${module.ecr.repository_url}:${var.app_image_tag}"

  environment_variables = [
    { name = "SPRING_PROFILES_ACTIVE", value = var.environment },
    { name = "DB_HOST",                value = module.database.endpoint },
    { name = "DB_PORT",                value = "5432" },
    { name = "DB_NAME",                value = var.db_name },
    { name = "MAIL_HOST",              value = var.mail_host },
    { name = "MAIL_PORT",              value = tostring(var.mail_port) },
    { name = "JWT_EXPIRATION_MS",      value = "86400000" },
    { name = "SERVER_PORT",            value = tostring(var.app_port) },
  ]

  secrets = [
    { name = "DB_USER",      valueFrom = "${module.secrets.secret_arn}:db_username::" },
    { name = "DB_PASSWORD",  valueFrom = "${module.secrets.secret_arn}:db_password::" },
    { name = "JWT_SECRET",   valueFrom = "${module.secrets.secret_arn}:jwt_secret::" },
    { name = "MAIL_USERNAME", valueFrom = "${module.secrets.secret_arn}:mail_username::" },
    { name = "MAIL_PASSWORD", valueFrom = "${module.secrets.secret_arn}:mail_password::" },
  ]

  secrets_arn = module.secrets.secret_arn
}

# ─── Landing page (S3 + CloudFront) ─────────────────────────────────────────
module "landing" {
  source = "./modules/landing"

  name_prefix     = local.name_prefix
  domain_name     = var.landing_domain_name
  certificate_arn = var.landing_certificate_arn
}

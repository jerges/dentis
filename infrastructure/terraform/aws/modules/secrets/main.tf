resource "aws_secretsmanager_secret" "app" {
  name                    = "${var.name_prefix}/app-secrets"
  description             = "Dentis application secrets"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    db_username  = var.db_username
    db_password  = var.db_password
    jwt_secret   = var.jwt_secret
    mail_username = var.mail_username
    mail_password = var.mail_password
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

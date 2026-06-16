# ── SNS: topic de alertas ────────────────────────────────────────────────────

resource "aws_sns_topic" "alerts" {
  name = "${var.app_name}-${var.environment}-alerts"
  tags = { Name = "${var.app_name}-${var.environment}-alerts" }
}

resource "aws_sns_topic_subscription" "email" {
  count     = var.alert_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# ── CloudWatch Log Groups (uno por servicio) ──────────────────────────────────

locals {
  log_services = toset(["app", "web", "landing", "postgres", "liquibase", "mailhog"])
}

resource "aws_cloudwatch_log_group" "service" {
  for_each          = local.log_services
  name              = "/dentis/${var.environment}/${each.key}"
  retention_in_days = 7
  tags              = { Service = each.key, Env = var.environment }
}

# ── Alarmas EC2 (métricas nativas — sin agente) ───────────────────────────────

resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name          = "${var.app_name}-${var.environment}-cpu-high"
  alarm_description   = "CPU > 80% durante 10 min"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
  dimensions          = { InstanceId = aws_instance.dev.id }
}

resource "aws_cloudwatch_metric_alarm" "status_check" {
  alarm_name          = "${var.app_name}-${var.environment}-status-check-failed"
  alarm_description   = "EC2 status check fallido"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Maximum"
  threshold           = 0
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
  dimensions          = { InstanceId = aws_instance.dev.id }
}

# ── Alarmas métricas custom del CloudWatch Agent ─────────────────────────────
# Requieren que el agente esté corriendo en el EC2.
# treat_missing_data = notBreaching → no alarman si el agente aún no reporta.

resource "aws_cloudwatch_metric_alarm" "mem_high" {
  alarm_name          = "${var.app_name}-${var.environment}-mem-high"
  alarm_description   = "Memoria > 85% durante 10 min"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "mem_used_percent"
  namespace           = "DentisDevEC2"
  period              = 300
  statistic           = "Average"
  threshold           = 85
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
  dimensions          = { InstanceId = aws_instance.dev.id }
}

resource "aws_cloudwatch_metric_alarm" "disk_high" {
  alarm_name          = "${var.app_name}-${var.environment}-disk-high"
  alarm_description   = "Disco / > 80% durante 10 min"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "disk_used_percent"
  namespace           = "DentisDevEC2"
  period              = 300
  statistic           = "Average"
  threshold           = 80
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
  # Amazon Linux 2023 monta / en xfs por defecto; drop_device=true en el agente
  # elimina la dimensión device, quedando sólo InstanceId + path + fstype.
  dimensions = {
    InstanceId = aws_instance.dev.id
    path       = "/"
    fstype     = "xfs"
  }
}

# ── Log metric filters: dentis-app container ─────────────────────────────────
# Estas alarmas se disparan basándose en los logs que Docker envía a CloudWatch
# via el driver awslogs definido en docker-compose.dev.yml

resource "aws_cloudwatch_log_metric_filter" "app_startup_failure" {
  name           = "${var.app_name}-${var.environment}-app-startup-failure"
  log_group_name = "/dentis/${var.environment}/app"
  # Spring Boot imprime exactamente esta cadena cuando no puede arrancar
  pattern        = "APPLICATION FAILED TO START"

  metric_transformation {
    name          = "AppStartupFailureCount"
    namespace     = "DentisDevEC2"
    value         = "1"
    default_value = "0"
  }

  depends_on = [aws_cloudwatch_log_group.service]
}

resource "aws_cloudwatch_metric_alarm" "app_startup_failed" {
  alarm_name          = "${var.app_name}-${var.environment}-app-startup-failed"
  alarm_description   = "dentis-api no levantó — APPLICATION FAILED TO START detectado en logs"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "AppStartupFailureCount"
  namespace           = "DentisDevEC2"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  # breaching: si no hay logs (contenedor no corrió) también se alarma
  treat_missing_data  = "breaching"
}

resource "aws_cloudwatch_log_metric_filter" "app_errors" {
  name           = "${var.app_name}-${var.environment}-app-errors"
  log_group_name = "/dentis/${var.environment}/app"
  # Captura líneas con ERROR, Exception o FATAL — ignora mayúsculas/minúsculas
  # con el operador ? (OR) de CloudWatch Logs filter syntax
  pattern        = "?ERROR ?Exception ?FATAL"

  metric_transformation {
    name          = "AppErrorCount"
    namespace     = "DentisDevEC2"
    value         = "1"
    default_value = "0"
  }

  depends_on = [aws_cloudwatch_log_group.service]
}

resource "aws_cloudwatch_metric_alarm" "app_errors_high" {
  alarm_name          = "${var.app_name}-${var.environment}-app-errors-high"
  alarm_description   = "dentis-api: >5 errores/excepciones en 5 minutos"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "AppErrorCount"
  namespace           = "DentisDevEC2"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
}

# ── Log metric filter: Liquibase ──────────────────────────────────────────────

resource "aws_cloudwatch_log_metric_filter" "liquibase_failure" {
  name           = "${var.app_name}-${var.environment}-liquibase-failure"
  log_group_name = "/dentis/${var.environment}/liquibase"
  # Liquibase reporta fallos con "SEVERE" o con el texto de error de Liquibase 4.x
  pattern        = "?SEVERE ?\"was unsuccessful\" ?\"Unexpected error running\""

  metric_transformation {
    name          = "LiquibaseFailureCount"
    namespace     = "DentisDevEC2"
    value         = "1"
    default_value = "0"
  }

  depends_on = [aws_cloudwatch_log_group.service]
}

resource "aws_cloudwatch_metric_alarm" "liquibase_failed" {
  alarm_name          = "${var.app_name}-${var.environment}-liquibase-failed"
  alarm_description   = "Liquibase falló al aplicar migraciones — dentis-api no podrá arrancar"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "LiquibaseFailureCount"
  namespace           = "DentisDevEC2"
  period              = 300
  statistic           = "Sum"
  threshold           = 0
  alarm_actions       = [aws_sns_topic.alerts.arn]
  ok_actions          = [aws_sns_topic.alerts.arn]
  treat_missing_data  = "notBreaching"
}

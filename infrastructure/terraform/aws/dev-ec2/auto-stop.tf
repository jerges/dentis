# ── Auto-stop: para el EC2 cuando lleva más de MAX_RUNTIME_HOURS encendido ─────
#
# EventBridge dispara la Lambda cada 15 minutos. La Lambda consulta el
# LaunchTime del EC2 y lo para si lleva >= max horas corriendo.

data "archive_file" "auto_stop" {
  type        = "zip"
  source_file = "${path.module}/lambda/auto_stop.py"
  output_path = "${path.module}/lambda/auto_stop.zip"
}

# ── IAM ──────────────────────────────────────────────────────────────────────

resource "aws_iam_role" "auto_stop_lambda" {
  name = "${var.app_name}-${var.environment}-auto-stop-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "auto_stop_lambda" {
  name = "auto-stop-policy"
  role = aws_iam_role.auto_stop_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ec2:DescribeInstances", "ec2:StopInstances"]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# ── Lambda ────────────────────────────────────────────────────────────────────

resource "aws_lambda_function" "auto_stop" {
  function_name    = "${var.app_name}-${var.environment}-auto-stop"
  role             = aws_iam_role.auto_stop_lambda.arn
  handler          = "auto_stop.handler"
  runtime          = "python3.12"
  filename         = data.archive_file.auto_stop.output_path
  source_code_hash = data.archive_file.auto_stop.output_base64sha256
  timeout          = 30

  environment {
    variables = {
      INSTANCE_ID       = aws_instance.dev.id
      MAX_RUNTIME_HOURS = tostring(var.auto_stop_max_runtime_hours)
    }
  }

  tags = {
    Name = "${var.app_name}-auto-stop"
  }
}

# ── EventBridge: dispara cada 15 min ─────────────────────────────────────────

resource "aws_cloudwatch_event_rule" "auto_stop" {
  name                = "${var.app_name}-${var.environment}-auto-stop"
  description         = "Para el EC2 dev si lleva mas de ${var.auto_stop_max_runtime_hours}h encendido"
  schedule_expression = "rate(15 minutes)"
}

resource "aws_cloudwatch_event_target" "auto_stop" {
  rule      = aws_cloudwatch_event_rule.auto_stop.name
  target_id = "AutoStopLambda"
  arn       = aws_lambda_function.auto_stop.arn
}

resource "aws_lambda_permission" "auto_stop_eventbridge" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auto_stop.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.auto_stop.arn
}

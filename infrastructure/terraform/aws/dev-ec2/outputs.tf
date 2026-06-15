locals {
  public_ip = var.assign_eip ? aws_eip.dev[0].public_ip : aws_instance.dev.public_ip
}

output "public_ip" {
  value       = local.public_ip
  description = "Public IP of EC2 instance"
}

output "public_dns" {
  value       = aws_instance.dev.public_dns
  description = "Public DNS of EC2 instance"
}

output "instance_id" {
  value       = aws_instance.dev.id
  description = "EC2 instance ID"
}

output "ssh_command" {
  value       = var.create_key_pair ? "ssh -i ${trimsuffix(pathexpand(var.ssh_public_key_path), ".pub")} ec2-user@${local.public_ip}" : "ssh ec2-user@${local.public_ip}"
  description = "SSH connection command"
}

output "ssm_start_session_command" {
  value       = "aws ssm start-session --target ${aws_instance.dev.id} --region ${var.aws_region}"
  description = "AWS Systems Manager Session Manager command"
}

output "web_url" {
  value       = "http://${local.public_ip}${var.web_port == 80 ? "" : ":${var.web_port}"}"
  description = "Frontend URL"
}

output "app_url" {
  value       = "http://${local.public_ip}:${var.app_port}"
  description = "Backend URL"
}

output "mailhog_url" {
  value       = "http://${local.public_ip}:8025"
  description = "MailHog URL"
}

output "cloudwatch_logs_url" {
  description = "CloudWatch Log Groups URL"
  value       = "https://console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#logsV2:log-groups$3FlogGroupNameFilter$3D%2Fdentis%2F${var.environment}"
}

output "cloudwatch_alarms_url" {
  description = "CloudWatch Alarms URL"
  value       = "https://console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#alarmsV2:"
}

output "sns_topic_arn" {
  description = "SNS topic ARN for alerts"
  value       = aws_sns_topic.alerts.arn
}

output "attachments_bucket_name" {
  description = "S3 bucket name for clinical attachments"
  value       = aws_s3_bucket.attachments.bucket
}

output "attachments_bucket_arn" {
  description = "S3 bucket ARN for clinical attachments"
  value       = aws_s3_bucket.attachments.arn
}

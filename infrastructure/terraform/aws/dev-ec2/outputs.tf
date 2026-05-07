locals {
  public_ip_or_eip = var.assign_eip ? aws_eip.dev[0].public_ip : aws_instance.dev.public_ip
}

output "public_ip" {
  value       = local.public_ip_or_eip
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
  value       = var.create_key_pair ? "ssh -i ${trimsuffix(pathexpand(var.ssh_public_key_path), ".pub")} ec2-user@${local.public_ip_or_eip}" : "ssh ec2-user@${local.public_ip_or_eip}"
  description = "SSH connection command"
}

output "ssm_start_session_command" {
  value       = "aws ssm start-session --target ${aws_instance.dev.id} --region ${var.aws_region}"
  description = "AWS Systems Manager Session Manager command"
}

output "web_url" {
  value       = "http://${local.public_ip_or_eip}${var.web_port == 80 ? "" : ":${var.web_port}"}"
  description = "Frontend URL"
}

output "app_url" {
  value       = "http://${local.public_ip_or_eip}:${var.app_port}"
  description = "Backend URL"
}

output "mailhog_url" {
  value       = "http://${local.public_ip_or_eip}:8025"
  description = "MailHog URL"
}


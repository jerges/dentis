output "public_ip" {
  value       = var.assign_eip ? aws_eip.dev[0].public_ip : aws_instance.dev.public_ip
  description = "Public IP of EC2 instance"
}

output "instance_id" {
  value       = aws_instance.dev.id
  description = "EC2 instance ID"
}

output "ssh_command" {
  value       = "ssh -i ~/.ssh/${var.app_name}-key ec2-user@${var.assign_eip ? aws_eip.dev[0].public_ip : aws_instance.dev.public_ip}"
  description = "SSH connection command"
}

output "ssm_start_session_command" {
  value       = "aws ssm start-session --target ${aws_instance.dev.id} --region ${var.aws_region}"
  description = "AWS Systems Manager Session Manager command"
}


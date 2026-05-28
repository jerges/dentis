output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = module.ecs.alb_dns_name
}

output "alb_url" {
  description = "HTTP URL to access the application"
  value       = "http://${module.ecs.alb_dns_name}"
}

output "ecr_repository_url" {
  description = "ECR repository URL to push Docker images"
  value       = module.ecr.repository_url
}

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = module.database.endpoint
  sensitive   = true
}

output "secrets_manager_arn" {
  description = "ARN of the Secrets Manager secret"
  value       = module.secrets.secret_arn
}

output "ecs_cluster_name" {
  description = "ECS Cluster name"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "ECS Service name"
  value       = module.ecs.service_name
}

output "docker_push_commands" {
  description = "Commands to build and push the Docker image"
  value       = <<-EOT
    # Authenticate Docker with ECR
    aws ecr get-login-password --region ${var.aws_region} | \
      docker login --username AWS --password-stdin ${module.ecr.repository_url}

    # Build from project root
    docker build -f infrastructure/docker/Dockerfile -t ${module.ecr.repository_url}:latest .

    # Push
    docker push ${module.ecr.repository_url}:latest

    # Force new ECS deployment
    aws ecs update-service \
      --cluster ${module.ecs.cluster_name} \
      --service ${module.ecs.service_name} \
      --force-new-deployment \
      --region ${var.aws_region}
  EOT
}

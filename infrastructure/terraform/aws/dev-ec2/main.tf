terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.app_name
      Environment = var.environment
      Stack       = "dev-ec2"
      ManagedBy   = "terraform"
    }
  }
}

# Find an AZ that supports the requested instance type (avoids us-east-1e gaps)
data "aws_ec2_instance_type_offerings" "available" {
  filter {
    name   = "instance-type"
    values = [var.instance_type]
  }
  location_type = "availability-zone"
}

locals {
  # Pick first AZ that supports the instance type, or fall back to var.availability_zone
  resolved_az = var.availability_zone != null ? var.availability_zone : tolist(data.aws_ec2_instance_type_offerings.available.locations)[0]
}

# Get latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name   = "root-device-type"
    values = ["ebs"]
  }
}

# VPC
resource "aws_vpc" "dev" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.app_name}-vpc"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "dev" {
  vpc_id = aws_vpc.dev.id

  tags = {
    Name = "${var.app_name}-igw"
  }
}

# Public Subnet
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.dev.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = local.resolved_az
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.app_name}-subnet"
  }
}

# Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.dev.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.dev.id
  }

  tags = {
    Name = "${var.app_name}-rt"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Security Group
resource "aws_security_group" "ec2" {
  name        = "${var.app_name}-sg"
  description = "Dev EC2 security group"
  vpc_id      = aws_vpc.dev.id

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_cidr]
    description = "SSH (optional when using Session Manager)"
  }

  # toset() deduplicates if landing_port == web_port (avoids duplicate rule error)
  dynamic "ingress" {
    for_each = toset([tostring(var.landing_port), tostring(var.web_port)])
    content {
      from_port   = tonumber(ingress.value)
      to_port     = tonumber(ingress.value)
      protocol    = "tcp"
      cidr_blocks = [var.web_allowed_cidr]
      description = "HTTP port ${ingress.value}"
    }
  }

  ingress {
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = [var.app_allowed_cidr]
    description = "Backend API"
  }

  ingress {
    from_port   = 8025
    to_port     = 8025
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MailHog"
  }

  ingress {
    from_port   = var.prometheus_port
    to_port     = var.prometheus_port
    protocol    = "tcp"
    cidr_blocks = [var.monitoring_allowed_cidr]
    description = "Prometheus"
  }

  ingress {
    from_port   = var.grafana_port
    to_port     = var.grafana_port
    protocol    = "tcp"
    cidr_blocks = [var.monitoring_allowed_cidr]
    description = "Grafana"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.app_name}-sg"
  }
}

# SSH Key Pair
resource "aws_key_pair" "dev" {
  count      = var.create_key_pair ? 1 : 0
  key_name   = "${var.app_name}-key"
  public_key = file(pathexpand(var.ssh_public_key_path))
}

# IAM Role for ECR and SSM access
resource "aws_iam_role" "ec2" {
  name = "${var.app_name}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecr" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# CloudWatch Agent (métricas de memoria/disco) + Logs (awslogs docker driver)
resource "aws_iam_role_policy_attachment" "cloudwatch_agent" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_role_policy_attachment" "cloudwatch_logs" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.app_name}-profile"
  role = aws_iam_role.ec2.name
}

# EC2 Instance - minimal, Docker installed only
resource "aws_instance" "dev" {
  ami                         = data.aws_ami.amazon_linux_2023.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.ec2.id]
  key_name                    = var.create_key_pair ? aws_key_pair.dev[0].key_name : var.existing_key_pair_name
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  associate_public_ip_address = true

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = "gp3"
    delete_on_termination = true
  }

  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -euo pipefail
    yum update -y
    yum install -y docker git amazon-ssm-agent amazon-cloudwatch-agent
    systemctl enable --now docker
    systemctl enable --now amazon-ssm-agent
    usermod -a -G docker ec2-user
    mkdir -p /opt/dentis
    chown ec2-user:ec2-user /opt/dentis

    # Docker Compose v2 plugin
    mkdir -p /usr/local/lib/docker/cli-plugins
    curl -SL \
      "https://github.com/docker/compose/releases/download/v2.27.1/docker-compose-linux-aarch64" \
      -o /usr/local/lib/docker/cli-plugins/docker-compose
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

    # CloudWatch Agent config
    mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
    cat > /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json <<'CWCONFIG'
    {
      "agent": { "metrics_collection_interval": 60 },
      "metrics": {
        "namespace": "DentisDevEC2",
        "append_dimensions": { "InstanceId": "$${aws:InstanceId}" },
        "metrics_collected": {
          "mem":  { "measurement": ["mem_used_percent"],  "metrics_collection_interval": 60 },
          "disk": { "measurement": ["disk_used_percent"], "resources": ["/"],
                    "metrics_collection_interval": 60, "drop_device": true }
        }
      }
    }
    CWCONFIG
    /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
      -a fetch-config -m ec2 -s \
      -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
  EOF
  )

  tags = {
    Name = "${var.app_name}-ec2"
  }
}

# ── S3 bucket for clinical attachments (images / X-rays) ─────────────────────
locals {
  attachments_bucket = var.attachments_bucket_name != "" ? var.attachments_bucket_name : "${var.app_name}-${var.environment}-attachments-${data.aws_caller_identity.current.account_id}"
}

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "attachments" {
  bucket        = local.attachments_bucket
  force_destroy = true

  tags = {
    Name = "${var.app_name}-attachments"
  }
}

resource "aws_s3_bucket_versioning" "attachments" {
  bucket = aws_s3_bucket.attachments.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "attachments" {
  bucket = aws_s3_bucket.attachments.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "attachments" {
  bucket                  = aws_s3_bucket.attachments.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "attachments" {
  bucket = aws_s3_bucket.attachments.id
  rule {
    id     = "expire-incomplete-uploads"
    status = "Enabled"
    abort_incomplete_multipart_upload { days_after_initiation = 7 }
    filter {}
  }
}

resource "aws_iam_policy" "s3_attachments" {
  name        = "${var.app_name}-${var.environment}-s3-attachments"
  description = "Allow EC2 app to read/write clinical attachments bucket"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject", "s3:HeadObject"]
        Resource = "${aws_s3_bucket.attachments.arn}/*"
      },
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.attachments.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "s3_attachments" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.s3_attachments.arn
}

# ── Bedrock: invoke models (Nova Pro, Titan Embeddings, Claude) ───────────────
resource "aws_iam_policy" "bedrock_invoke" {
  name        = "${var.app_name}-${var.environment}-bedrock-invoke"
  description = "Allow EC2 app to invoke Bedrock models and use Converse API"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "bedrock:InvokeModel",
          "bedrock:InvokeModelWithResponseStream"
        ]
        Resource = "arn:aws:bedrock:${var.aws_region}::foundation-model/*"
      },
      {
        Effect = "Allow"
        Action = [
          "bedrock:Converse",
          "bedrock:ConverseStream"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "bedrock_invoke" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.bedrock_invoke.arn
}

# Elastic IP — IP fija aunque la instancia se apague/enciende
resource "aws_eip" "dev" {
  count    = var.assign_eip ? 1 : 0
  instance = aws_instance.dev.id
  domain   = "vpc"

  tags = {
    Name = "${var.app_name}-eip"
  }
}



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

# Get latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*"]
  }
}

# VPC
resource "aws_vpc" "dev" {
  cidr_block           = "10.60.0.0/16"
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
  cidr_block              = "10.60.1.0/24"
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

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Backend"
  }

  ingress {
    from_port   = 8025
    to_port     = 8025
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "MailHog"
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

  # Install Docker and ensure SSM Agent is running
  user_data = base64encode(<<-EOF
    #!/bin/bash
    set -euo pipefail
    yum update -y
    yum install -y docker git amazon-ssm-agent
    systemctl start docker
    systemctl enable docker
    systemctl enable amazon-ssm-agent
    systemctl start amazon-ssm-agent
    usermod -a -G docker ec2-user
  EOF
  )

  tags = {
    Name = "${var.app_name}-ec2"
  }
}

# Elastic IP (optional)
resource "aws_eip" "dev" {
  count    = var.assign_eip ? 1 : 0
  instance = aws_instance.dev.id
  domain   = "vpc"

  tags = {
    Name = "${var.app_name}-eip"
  }
}


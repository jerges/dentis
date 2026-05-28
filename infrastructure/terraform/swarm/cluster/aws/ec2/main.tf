terraform {
    required_providers {
        aws = {
            source  = "hashicorp/aws"
            version = "~> 3.0"
        }
    }
}

output "instance_public_dns" {
    value = aws_instance.findPark.public_dns
}

provider "aws" {
    region     = var.region
    access_key = var.my_access_key
    secret_key = var.my_secret_key
}


output "access_key_is" {
    value = var.my_access_key
}

output "secret_key_is" {
    value = var.my_secret_key
}


resource "aws_instance" "findPark" {
    ami                         = "ami-04a92520784b93e73"
    instance_type               = "t2.micro"
    key_name                    = "mac-jb-key"
    monitoring                  = true
    associate_public_ip_address = true
    tags = {
        Name = "EC2 Find Park backoffice"
    }
    subnet_id = aws_subnet.sba_subnet_1.id
    vpc_security_group_ids = [aws_security_group.external.id]
    user_data = <<-EOF
            #!/bin/bash
            sudo apt update -y
            sudo apt install -y docker.io
            sudo systemctl start docker
            sudo chown root:docker /var/run/docker.sock
            sudo chmod 660 /var/run/docker.sock
            sudo usermod -a -G docker ubuntu
            newgrp docker
            sudo apt install unzip -y
            sudo apt install curl -y
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            sudo ./aws/install

            # Configurar AWS CLI con aws configure
            export AWS_ACCESS_KEY_ID=${var.my_access_key}
            export AWS_SECRET_ACCESS_KEY=${var.my_secret_key}
            mkdir -p /home/ubuntu/.aws

            aws configure set aws_access_key_id ${var.my_access_key} --profile default
            aws configure set aws_secret_access_key ${var.my_secret_key} --profile default
            aws configure set region eu-west-3 --profile default
            aws configure set output json --profile default

            chown -R ubuntu:ubuntu /home/ubuntu/.aws

            # Login en ECR usando AWS CLI
            aws ecr get-login-password --region eu-west-3 | sudo docker login --username AWS --password-stdin ${var.repository_url}

            # Descargar e iniciar la imagen Docker
            sudo docker pull ${var.repository_url}:latest

            # Instalar y configurar agente de CloudWatch
            sudo apt install -y amazon-cloudwatch-agent
            cat <<EOT > /opt/aws/amazon-cloudwatch-agent/bin/config.json
            {
              "metrics": {
                "append_dimensions": {
                  "InstanceId": "$${aws:InstanceId}"
                },
                "aggregation_dimensions": [["InstanceId"]],
                "metrics_collected": {
                  "cpu": {
                    "measurement": [
                      {"name": "cpu_usage_idle", "unit": "Percent"}
                    ],
                    "resources": ["*"]
                  },
                  "disk": {
                    "measurement": [
                      "used_percent"
                    ],
                    "resources": ["*"]
                  },
                  "memory": {
                    "measurement": [
                      "mem_used_percent"
                    ]
                  }
                }
              }
            }
            EOT

            # Iniciar el agente de CloudWatch
            sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json -s
            EOF
}


resource "aws_key_pair" "jb_key" {
    key_name = var.certificate_name
    public_key = file(var.certificate_public_url)
}

resource "aws_security_group" "external" {
    name        = "http-access-external"
    description = "Permitir acceso HTTP y HTTPS"
    vpc_id      = aws_vpc.main.id

    ingress {
        description = "Allow HTTP"
        from_port   = 80
        to_port     = 80
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    # Agregar también HTTPS si es necesario
    ingress {
        from_port = 443
        to_port   = 443
        protocol  = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    ingress {
        description = "Allow SSH"
        from_port   = 22
        to_port     = 22
        protocol    = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }

    egress {
        from_port = 0
        to_port   = 0
        protocol  = "-1"
        cidr_blocks = ["0.0.0.0/0"]
    }
    tags = {
        Name = "http-access-external"
    }
}

resource "aws_internet_gateway" "main" {
    vpc_id = aws_vpc.main.id
    tags = {
        Name = "main-igw"
    }
}

resource "aws_route_table" "main" {
    vpc_id = aws_vpc.main.id

    route {
        cidr_block = "0.0.0.0/0"
        gateway_id = aws_internet_gateway.main.id
    }

    tags = {
        Name = "main-route-table"
    }
}

resource "aws_route_table_association" "subnet_1" {
    subnet_id      = aws_subnet.sba_subnet_1.id
    route_table_id = aws_route_table.main.id
}

resource "aws_route_table_association" "subnet_2" {
    subnet_id      = aws_subnet.sba_subnet_2.id
    route_table_id = aws_route_table.main.id
}

resource "aws_route_table_association" "subnet_3" {
    subnet_id      = aws_subnet.sba_subnet_3.id
    route_table_id = aws_route_table.main.id
}


resource "aws_lb" "sba_lb" {
    name               = "sba-lb"
    internal           = false
    load_balancer_type = "application"
    security_groups = [aws_security_group.external.id]
    subnets = [aws_subnet.sba_subnet_1.id, aws_subnet.sba_subnet_2.id, aws_subnet.sba_subnet_3.id]

    enable_deletion_protection = false

    tags = {
        Environment = var.environment_tag
    }
}

resource "aws_lb_target_group" "sba_target_group" {
    name        = "sba-target-group"
    port        = 8080
    protocol    = "HTTP"
    vpc_id      = aws_vpc.main.id
    target_type = "instance"

    health_check {
        path                = "/"
        interval            = 30
        timeout             = 5
        healthy_threshold   = 2
        unhealthy_threshold = 2
    }
}

resource "aws_lb_listener" "http" {
    load_balancer_arn = aws_lb.sba_lb.arn
    port              = 80
    protocol          = "HTTP"

    default_action {
        type             = "forward"
        target_group_arn = aws_lb_target_group.sba_target_group.arn
    }
}

resource "aws_lb_target_group_attachment" "sba_attachment_group" {
    target_group_arn = aws_lb_target_group.sba_target_group.arn
    target_id        = aws_instance.findPark.id
    port             = 80
}

resource "aws_vpc" "main" {
    cidr_block           = "10.0.0.0/16"
    enable_dns_support   = true
    enable_dns_hostnames = true
    tags = {
        Name = "main-vpc"
    }
}

resource "aws_subnet" "sba_subnet_1" {
    vpc_id                  = aws_vpc.main.id
    cidr_block              = "10.0.1.0/24"
    availability_zone       = "eu-west-3a"
    map_public_ip_on_launch = true

    tags = {
        Name = "sba-subnet-1"
    }
}

resource "aws_subnet" "sba_subnet_2" {
    vpc_id                  = aws_vpc.main.id
    cidr_block              = "10.0.2.0/24"
    availability_zone       = "eu-west-3b"
    map_public_ip_on_launch = true

    tags = {
        Name = "sba-subnet-2"
    }
}

resource "aws_subnet" "sba_subnet_3" {
    vpc_id                  = aws_vpc.main.id
    cidr_block              = "10.0.3.0/24"
    availability_zone       = "eu-west-3c"
    map_public_ip_on_launch = true

    tags = {
        Name = "sba-subnet-3"
    }
}






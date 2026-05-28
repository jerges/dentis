output "access_key_is" {
    value = var.my_access_key
}

output "secret_key_is" {
    value = var.my_secret_key
}

provider "aws" {
    region     = var.region
    access_key = var.my_access_key
    secret_key = var.my_secret_key
}

resource "aws_ecr_repository" "find_park_repository" {
    name = var.repository_name
}

output "repository_url" {
    value = aws_ecr_repository.find_park_repository.repository_url
}

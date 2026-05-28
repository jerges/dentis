terraform {
    required_providers {
        docker = {
            source  = "kreuzwerker/docker"
            version = "~> 3.0"
        }
    }
}

provider "docker" {
    host = "ssh://ubuntu@${var.public_dns}"
    ssh_opts = [
        "-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null", "-i", var.certificate_private_url
    ]

}

# Red Docker
resource "docker_network" "app_network" {
    name = "app_network"
}

# Contenedor MongoDB
resource "docker_container" "mongodb" {
    image = "mongo:latest"
    name  = "mongodb"
    ports {
        internal = 27017
        external = 27017
    }
    networks_advanced {
        name = docker_network.app_network.name
    }
    healthcheck {
        test = ["CMD", "mongo", "--eval", "db.runCommand({ ping: 1 })"]
        interval = "5s"
        timeout  = "5s"
        retries  = 3
    }
}

resource "docker_image" "app_image" {
    name         = "${var.docker_image}:latest"
    keep_locally = false
}

# Contenedor App
resource "docker_container" "app" {
    image = docker_image.app_image.name
    name  = var.docker_name
    ports {
        internal = 8080
        external = 80
    }
    env = [
        "_JAVA_OPTIONS=-Xmx512m -Xms256m",
        "SPRING_PROFILES_ACTIVE=prod,api-docs",
        "MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true",
        "SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/portal?waitQueueMultiple=1000",
    ]
    networks_advanced {
        name = docker_network.app_network.name
    }
    healthcheck {
        test = ["CMD", "curl", "-f", "http://localhost:8080/management/health"]
        interval = "5s"
        timeout  = "5s"
        retries  = 40
    }
    depends_on = [
        docker_image.app_image,
        docker_container.mongodb
    ]
}





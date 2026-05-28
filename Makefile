.PHONY: help ecr-login up down restart pull ps logs config reset-db

DEV_COMPOSE_SCRIPT := ./scripts/dev-compose.sh
SERVICE ?=

help:
	@echo "Available targets:"
	@echo "  make ecr-login  - Log Docker into the ECR registry used by .env.local"
	@echo "  make up         - Start the local stack from docker-compose.dev.yml"
	@echo "  make down       - Stop the local stack"
	@echo "  make restart    - Restart the local stack"
	@echo "  make pull       - Pull backend and frontend images"
	@echo "  make ps         - Show container status"
	@echo "  make logs       - Follow logs for all services"
	@echo "  make logs SERVICE=app - Follow logs for a single service"
	@echo "  make config     - Print resolved Docker Compose configuration"
	@echo "  make reset-db   - Recreate the stack and reset PostgreSQL volume"

ecr-login:
	@bash $(DEV_COMPOSE_SCRIPT) ecr-login

up:
	@bash $(DEV_COMPOSE_SCRIPT) up

down:
	@bash $(DEV_COMPOSE_SCRIPT) down

restart:
	@bash $(DEV_COMPOSE_SCRIPT) restart

pull:
	@bash $(DEV_COMPOSE_SCRIPT) pull

ps:
	@bash $(DEV_COMPOSE_SCRIPT) ps

logs:
	@bash $(DEV_COMPOSE_SCRIPT) logs $(SERVICE)

config:
	@bash $(DEV_COMPOSE_SCRIPT) config

reset-db:
	@bash $(DEV_COMPOSE_SCRIPT) reset-db


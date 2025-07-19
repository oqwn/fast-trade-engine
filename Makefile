.PHONY: help build test dev prod clean install lint format

# Default target
help:
	@echo "Fast Trade Engine - Makefile Commands"
	@echo "===================================="
	@echo "install        - Install all dependencies (frontend & backend)"
	@echo "build          - Build both frontend and backend"
	@echo "test           - Run all tests"
	@echo "dev            - Start development environment"
	@echo "prod           - Start production environment"
	@echo "stop           - Stop all containers"
	@echo "clean          - Clean build artifacts and containers"
	@echo "lint           - Run linters for both frontend and backend"
	@echo "format         - Format code"
	@echo "logs           - Show logs from all containers"
	@echo "backend-*      - Backend specific commands"
	@echo "frontend-*     - Frontend specific commands"

# Install dependencies
install:
	@echo "Installing backend dependencies..."
	cd backend && mvn clean install -DskipTests
	@echo "Installing frontend dependencies..."
	cd frontend && npm install

# Build everything
build: backend-build frontend-build

# Run all tests
test: backend-test frontend-test

# Development environment
dev:
	docker-compose -f docker-compose.dev.yml up --build

dev-down:
	docker-compose -f docker-compose.dev.yml down

# Production environment
prod:
	docker-compose up --build -d

prod-down:
	docker-compose down

# Stop all containers
stop:
	docker-compose down
	docker-compose -f docker-compose.dev.yml down

# Clean everything
clean: stop
	cd backend && mvn clean
	cd frontend && rm -rf node_modules dist
	docker system prune -f

# Linting
lint: backend-lint frontend-lint

# Format code
format: backend-format frontend-format

# Show logs
logs:
	docker-compose logs -f

logs-backend:
	docker-compose logs -f backend

logs-frontend:
	docker-compose logs -f frontend

# Backend specific commands
backend-build:
	cd backend && mvn clean package -DskipTests

backend-test:
	cd backend && mvn test

backend-run:
	cd backend && mvn spring-boot:run

backend-lint:
	cd backend && mvn checkstyle:check spotbugs:check pmd:check

backend-format:
	cd backend && mvn spotless:apply

backend-docker:
	docker build -t fast-trade-engine-backend:latest ./backend

# Frontend specific commands
frontend-build:
	cd frontend && npm run build

frontend-test:
	cd frontend && npm test

frontend-run:
	cd frontend && npm run dev

frontend-lint:
	cd frontend && npm run lint

frontend-format:
	cd frontend && npm run format

frontend-docker:
	docker build -t fast-trade-engine-frontend:latest ./frontend

# Database commands
db-reset:
	docker-compose exec postgres psql -U fasttrader -d fasttrader -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
	docker-compose restart backend

db-shell:
	docker-compose exec postgres psql -U fasttrader -d fasttrader

# Monitoring
monitor-start:
	docker-compose up -d prometheus grafana

monitor-stop:
	docker-compose stop prometheus grafana

# Health checks
health-check:
	@echo "Checking backend health..."
	@curl -f http://localhost:8080/api/actuator/health || echo "Backend is not healthy"
	@echo "\nChecking frontend health..."
	@curl -f http://localhost:3000/health || echo "Frontend is not healthy"
	@echo "\nChecking database health..."
	@docker-compose exec postgres pg_isready || echo "Database is not healthy"

# Performance benchmarks
benchmark:
	cd backend && mvn clean test-compile && mvn exec:java -Dexec.mainClass="org.openjdk.jmh.Main" -Dexec.classpathScope=test

# Generate documentation
docs:
	cd backend && mvn javadoc:javadoc
	cd frontend && npm run build-storybook

# Docker compose override for different environments
dev-with-monitoring:
	docker-compose -f docker-compose.dev.yml -f docker-compose.monitoring.yml up

prod-scaled:
	docker-compose up -d --scale backend=3

# Utility commands
shell-backend:
	docker-compose exec backend /bin/sh

shell-frontend:
	docker-compose exec frontend /bin/sh

# Quick start commands
quickstart: install
	@echo "Starting Fast Trade Engine..."
	@echo "Backend will be available at: http://localhost:8080"
	@echo "Frontend will be available at: http://localhost:3000"
	@echo "Grafana will be available at: http://localhost:3001 (admin/admin)"
	docker-compose up --build
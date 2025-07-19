#!/bin/bash

# Fast Trade Engine Deployment Script
set -e

echo "🚀 Fast Trade Engine Deployment Script"
echo "======================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Default values
ENVIRONMENT="development"
COMPOSE_FILE="docker-compose.yml"
SCALE_REPLICAS=1

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --scale)
            SCALE_REPLICAS="$2"
            shift 2
            ;;
        --prod)
            ENVIRONMENT="production"
            shift
            ;;
        --dev)
            ENVIRONMENT="development"
            shift
            ;;
        --staging)
            ENVIRONMENT="staging"
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -e, --environment ENV   Set environment (development, staging, production)"
            echo "  --scale N               Scale application to N replicas"
            echo "  --prod                  Deploy to production"
            echo "  --dev                   Deploy to development"
            echo "  --staging               Deploy to staging"
            echo "  -h, --help              Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set compose file based on environment
case $ENVIRONMENT in
    development)
        COMPOSE_FILE="docker-compose.dev.yml"
        ;;
    staging)
        COMPOSE_FILE="docker-compose.yml"
        ;;
    production)
        COMPOSE_FILE="docker-compose.prod.yml"
        ;;
    *)
        log_error "Unknown environment: $ENVIRONMENT"
        exit 1
        ;;
esac

log_info "Deploying to environment: $ENVIRONMENT"
log_info "Using compose file: $COMPOSE_FILE"

# Check if Docker and Docker Compose are installed
if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose is not installed or not in PATH"
    exit 1
fi

# Check if compose file exists
if [ ! -f "$COMPOSE_FILE" ]; then
    log_error "Compose file not found: $COMPOSE_FILE"
    exit 1
fi

# Create production compose file if it doesn't exist
if [ "$ENVIRONMENT" = "production" ] && [ ! -f "docker-compose.prod.yml" ]; then
    log_info "Creating production compose file..."
    cp docker-compose.yml docker-compose.prod.yml
    # Add production-specific configurations
    cat >> docker-compose.prod.yml << EOF

  # Production overrides
  app:
    restart: always
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - JAVA_OPTS=-server -Xmx2g -Xms1g
    deploy:
      replicas: $SCALE_REPLICAS
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 1G
          cpus: '0.5'
EOF
fi

# Environment-specific pre-deployment checks
case $ENVIRONMENT in
    production)
        log_info "Running production pre-deployment checks..."
        
        # Check if all required environment variables are set
        if [ -z "$DATABASE_URL" ]; then
            log_warn "DATABASE_URL not set, using default"
        fi
        
        if [ -z "$REDIS_URL" ]; then
            log_warn "REDIS_URL not set, using default"
        fi
        
        # Backup database (if needed)
        log_info "Consider backing up database before deployment"
        ;;
    staging)
        log_info "Running staging pre-deployment checks..."
        ;;
    development)
        log_info "Running development setup..."
        ;;
esac

# Pull latest images
log_info "Pulling latest images..."
docker-compose -f $COMPOSE_FILE pull

# Stop existing containers
log_info "Stopping existing containers..."
docker-compose -f $COMPOSE_FILE down

# Remove orphaned containers
log_info "Removing orphaned containers..."
docker-compose -f $COMPOSE_FILE down --remove-orphans

# Start services
log_info "Starting services..."
if [ $SCALE_REPLICAS -gt 1 ]; then
    docker-compose -f $COMPOSE_FILE up -d --scale app=$SCALE_REPLICAS
else
    docker-compose -f $COMPOSE_FILE up -d
fi

# Wait for services to be healthy
log_info "Waiting for services to be healthy..."
sleep 30

# Check service health
log_info "Checking service health..."
docker-compose -f $COMPOSE_FILE ps

# Run health checks
log_info "Running health checks..."
for i in {1..30}; do
    if curl -f http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
        log_info "Application is healthy! ✅"
        break
    else
        log_debug "Health check attempt $i/30 failed, retrying in 5 seconds..."
        sleep 5
    fi
    
    if [ $i -eq 30 ]; then
        log_error "Application failed to become healthy"
        log_info "Checking logs..."
        docker-compose -f $COMPOSE_FILE logs app
        exit 1
    fi
done

# Show running services
log_info "Deployment completed successfully! ✅"
echo
log_info "Running services:"
docker-compose -f $COMPOSE_FILE ps

echo
log_info "Service endpoints:"
echo "  • Application: http://localhost:8080/api"
echo "  • Health check: http://localhost:8080/api/actuator/health"
echo "  • Metrics: http://localhost:8080/api/actuator/metrics"
echo "  • Prometheus: http://localhost:9090"
echo "  • Grafana: http://localhost:3000 (admin/admin)"

if [ "$ENVIRONMENT" = "development" ]; then
    echo "  • H2 Console: http://localhost:8080/api/h2-console"
    echo "  • MailHog: http://localhost:8025"
fi

echo
log_info "Useful commands:"
echo "  • View logs: docker-compose -f $COMPOSE_FILE logs -f"
echo "  • Stop services: docker-compose -f $COMPOSE_FILE down"
echo "  • Restart app: docker-compose -f $COMPOSE_FILE restart app"
echo "  • Scale app: docker-compose -f $COMPOSE_FILE up -d --scale app=N"
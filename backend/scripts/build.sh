#!/bin/bash

# Fast Trade Engine Build Script
set -e

echo "🚀 Fast Trade Engine Build Script"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Parse command line arguments
SKIP_TESTS=false
SKIP_QUALITY=false
BUILD_DOCKER=false
PUSH_DOCKER=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-quality)
            SKIP_QUALITY=true
            shift
            ;;
        --docker)
            BUILD_DOCKER=true
            shift
            ;;
        --push)
            PUSH_DOCKER=true
            BUILD_DOCKER=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --skip-tests    Skip running tests"
            echo "  --skip-quality  Skip quality checks"
            echo "  --docker        Build Docker image"
            echo "  --push          Build and push Docker image"
            echo "  -h, --help      Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    log_error "Maven is not installed or not in PATH"
    exit 1
fi

# Check if Docker is installed (if needed)
if [ "$BUILD_DOCKER" = true ] && ! command -v docker &> /dev/null; then
    log_error "Docker is not installed or not in PATH"
    exit 1
fi

# Clean previous builds
log_info "Cleaning previous builds..."
mvn clean

# Compile
log_info "Compiling source code..."
mvn compile

# Run tests
if [ "$SKIP_TESTS" = false ]; then
    log_info "Running tests..."
    mvn test
    
    log_info "Generating test reports..."
    mvn jacoco:report
else
    log_warn "Skipping tests"
fi

# Quality checks
if [ "$SKIP_QUALITY" = false ]; then
    log_info "Running quality checks..."
    
    if [ "$SKIP_TESTS" = false ]; then
        log_info "Running Checkstyle..."
        mvn checkstyle:check || log_warn "Checkstyle issues found"
        
        log_info "Running SpotBugs..."
        mvn spotbugs:check || log_warn "SpotBugs issues found"
        
        log_info "Running PMD..."
        mvn pmd:check || log_warn "PMD issues found"
    else
        log_warn "Skipping quality checks (tests skipped)"
    fi
else
    log_warn "Skipping quality checks"
fi

# Package
log_info "Packaging application..."
if [ "$SKIP_TESTS" = true ]; then
    mvn package -DskipTests
else
    mvn package
fi

# Build Docker image
if [ "$BUILD_DOCKER" = true ]; then
    log_info "Building Docker image..."
    docker build -t fast-trade-engine:latest .
    
    # Tag with version
    VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    docker tag fast-trade-engine:latest fast-trade-engine:$VERSION
    
    log_info "Docker image built: fast-trade-engine:latest, fast-trade-engine:$VERSION"
    
    # Push to registry
    if [ "$PUSH_DOCKER" = true ]; then
        log_info "Pushing Docker image..."
        if [ -z "$DOCKER_REGISTRY" ]; then
            log_warn "DOCKER_REGISTRY not set, pushing to Docker Hub"
            docker push fast-trade-engine:latest
            docker push fast-trade-engine:$VERSION
        else
            docker tag fast-trade-engine:latest $DOCKER_REGISTRY/fast-trade-engine:latest
            docker tag fast-trade-engine:$VERSION $DOCKER_REGISTRY/fast-trade-engine:$VERSION
            docker push $DOCKER_REGISTRY/fast-trade-engine:latest
            docker push $DOCKER_REGISTRY/fast-trade-engine:$VERSION
        fi
    fi
fi

log_info "Build completed successfully! ✅"

# Display build artifacts
echo
log_info "Build artifacts:"
ls -la target/*.jar 2>/dev/null || log_warn "No JAR files found"

if [ "$BUILD_DOCKER" = true ]; then
    echo
    log_info "Docker images:"
    docker images fast-trade-engine
fi

echo
log_info "Next steps:"
echo "  • Run the application: java -jar target/fast-trade-engine-*.jar"
echo "  • Run with Docker: docker run -p 8080:8080 fast-trade-engine:latest"
echo "  • Run with Docker Compose: docker-compose up"
echo "  • Access API docs: http://localhost:8080/api"
echo "  • Access H2 console: http://localhost:8080/api/h2-console"
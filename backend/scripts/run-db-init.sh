#!/bin/bash

# =====================================================
# Fast Trade Engine - Database Initialization Script
# =====================================================
# This script initializes the complete database with schema,
# indexes, sample data, and migration tracking.

set -e  # Exit on any error

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-fast_trade_engine}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-postgres}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if PostgreSQL is available
check_postgres() {
    log "Checking PostgreSQL connection..."
    
    if ! command -v psql &> /dev/null; then
        error "psql command not found. Please install PostgreSQL client."
        exit 1
    fi
    
    # Test connection
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" &> /dev/null; then
        error "Cannot connect to PostgreSQL. Please check your connection settings."
        error "Host: $DB_HOST, Port: $DB_PORT, User: $DB_USER"
        exit 1
    fi
    
    success "PostgreSQL connection successful"
}

# Create database if it doesn't exist
create_database() {
    log "Creating database '$DB_NAME' if it doesn't exist..."
    
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "
        SELECT 'CREATE DATABASE $DB_NAME' 
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME')\\gexec
    " || {
        warning "Database might already exist, continuing..."
    }
    
    success "Database '$DB_NAME' is ready"
}

# Run SQL script
run_sql_script() {
    local script_file="$1"
    local description="$2"
    
    if [ ! -f "$script_file" ]; then
        error "Script file not found: $script_file"
        return 1
    fi
    
    log "$description..."
    log "Executing: $script_file"
    
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$script_file"; then
        success "$description completed successfully"
        return 0
    else
        error "$description failed"
        return 1
    fi
}

# Check migration status
check_migrations() {
    log "Checking migration status..."
    
    local migration_exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
        SELECT EXISTS (
            SELECT FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name = 'migrations'
        );
    " 2>/dev/null | tr -d ' ')
    
    if [ "$migration_exists" = "t" ]; then
        log "Migrations table exists. Checking applied migrations..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
            SELECT version, description, executed_at 
            FROM migrations 
            ORDER BY executed_at;
        "
    else
        log "Migrations table doesn't exist. This appears to be a fresh installation."
    fi
}

# Validate installation
validate_installation() {
    log "Validating database installation..."
    
    # Check essential tables
    local tables=("users" "accounts" "instruments" "orders" "trades" "positions" "market_data_ticks")
    local missing_tables=()
    
    for table in "${tables[@]}"; do
        local exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = '$table'
            );
        " 2>/dev/null | tr -d ' ')
        
        if [ "$exists" = "t" ]; then
            success "Table '$table' exists"
        else
            error "Table '$table' missing"
            missing_tables+=("$table")
        fi
    done
    
    if [ ${#missing_tables[@]} -eq 0 ]; then
        success "All essential tables are present"
        
        # Check sample data
        local user_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d ' ')
        local instrument_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM instruments;" 2>/dev/null | tr -d ' ')
        
        log "Sample data summary:"
        log "  - Users: $user_count"
        log "  - Instruments: $instrument_count"
        
        return 0
    else
        error "Missing tables: ${missing_tables[*]}"
        return 1
    fi
}

# Show database info
show_database_info() {
    log "Database connection information:"
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"
    echo "  Database: $DB_NAME"
    echo "  User: $DB_USER"
    echo ""
}

# Main execution
main() {
    echo "=============================================="
    echo "Fast Trade Engine - Database Initialization"
    echo "=============================================="
    echo ""
    
    show_database_info
    
    # Parse command line arguments
    FORCE_RECREATE=false
    SKIP_SAMPLE_DATA=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --force-recreate)
                FORCE_RECREATE=true
                warning "Force recreate mode enabled - this will drop existing data!"
                shift
                ;;
            --skip-sample-data)
                SKIP_SAMPLE_DATA=true
                log "Skipping sample data population"
                shift
                ;;
            --help)
                echo "Usage: $0 [OPTIONS]"
                echo ""
                echo "Options:"
                echo "  --force-recreate    Drop and recreate all tables (WARNING: destroys data)"
                echo "  --skip-sample-data  Skip populating sample data"
                echo "  --help             Show this help message"
                echo ""
                echo "Environment variables:"
                echo "  DB_HOST            Database host (default: localhost)"
                echo "  DB_PORT            Database port (default: 5432)"
                echo "  DB_NAME            Database name (default: fast_trade_engine)"
                echo "  DB_USER            Database user (default: postgres)"
                echo "  DB_PASSWORD        Database password (default: postgres)"
                exit 0
                ;;
            *)
                error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    # Step 1: Check PostgreSQL connection
    check_postgres
    
    # Step 2: Create database
    create_database
    
    # Step 3: Check current migration status
    check_migrations
    
    # Step 4: Initialize schema
    if [ "$FORCE_RECREATE" = true ]; then
        warning "Recreating database schema..."
        run_sql_script "$SCRIPT_DIR/db-init-complete.sql" "Creating database schema (force recreate)"
    else
        # Check if tables exist
        local tables_exist=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
            SELECT EXISTS (
                SELECT FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_name = 'users'
            );
        " 2>/dev/null | tr -d ' ')
        
        if [ "$tables_exist" = "t" ]; then
            log "Database schema already exists. Skipping schema creation."
            log "Use --force-recreate to recreate the schema."
        else
            run_sql_script "$SCRIPT_DIR/db-init-complete.sql" "Creating database schema"
        fi
    fi
    
    # Step 5: Populate sample data
    if [ "$SKIP_SAMPLE_DATA" = false ]; then
        if [ "$FORCE_RECREATE" = true ]; then
            run_sql_script "$SCRIPT_DIR/db-sample-data.sql" "Populating sample data"
        else
            # Check if sample data exists
            local sample_data_exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
                SELECT COUNT(*) > 0 FROM users WHERE username = 'demo_user';
            " 2>/dev/null | tr -d ' ')
            
            if [ "$sample_data_exists" = "t" ]; then
                log "Sample data already exists. Skipping sample data population."
                log "Use --force-recreate to recreate sample data."
            else
                run_sql_script "$SCRIPT_DIR/db-sample-data.sql" "Populating sample data"
            fi
        fi
    fi
    
    # Step 6: Validate installation
    if validate_installation; then
        echo ""
        echo "=============================================="
        success "Database initialization completed successfully!"
        echo "=============================================="
        echo ""
        log "You can now:"
        log "1. Start the backend application"
        log "2. Connect to the database using:"
        log "   psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"
        log "3. Access the sample trading accounts in the application"
        echo ""
        log "Sample login credentials:"
        log "  Username: demo_user"
        log "  Password: demo123 (application will handle this)"
        echo ""
    else
        error "Database initialization failed validation!"
        exit 1
    fi
}

# Run main function with all arguments
main "$@"
# Deployment Guide

This guide explains how to deploy the Library Management API (Spring Boot 4.0.1 + PostgreSQL) using Docker Compose.

## Architecture

The stack includes:
- **Spring Boot 4.0.1 Application**: Java 21 REST API running on port 8080 (internal)
- **PostgreSQL 16**: Database with persistent storage via Docker volume
- **Docker Compose**: Container orchestration for multi-container deployment

## Quick Start

### 1. Prerequisites

- Docker and Docker Compose installed
- `.env` file configured (copy from `.env.example`)

### 2. Configure Environment

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your settings
# At minimum, change these:
# - POSTGRES_PASSWORD (Database password)
# - JWT_SECRET_KEY (JWT signing secret, 256-bit / 64 hex chars)
# - ADMIN_PASSWORD (Initial admin user password)
# - LIBRARIAN_PASSWORD (Initial librarian user password)
# - MEMBER_PASSWORD (Initial member user password)
```

**Important Environment Variables:**

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `POSTGRES_SERVER` | Database host | Yes | `db` (in Docker) |
| `POSTGRES_PORT` | Database port | Yes | `5432` |
| `POSTGRES_DB` | Database name | Yes | `library_db` |
| `POSTGRES_USER` | Database user | Yes | `postgres` |
| `POSTGRES_PASSWORD` | Database password | Yes | `your_secure_password_here` |
| `JWT_SECRET_KEY` | JWT signing key (256-bit HEX) | Yes | See `.env.example` |
| `JWT_EXPIRATION` | Token expiration (milliseconds) | No | `3600000` (1 hour) |
| `JWT_ISSUER` | JWT token issuer | No | `library-api` |
| `JWT_AUDIENCE` | JWT token audience | No | `library-api-client` |
| `DDL_AUTO` | Hibernate DDL mode | No | `update` (dev), `validate` (prod) |
| `ADMIN_EMAIL` | Admin user email | Yes | `admin@library.com` |
| `ADMIN_PASSWORD` | Admin user password | Yes | `YourSecurePassword123!` |
| `ADMIN_FULLNAME` | Admin user full name | No | `System Administrator` |
| `LIBRARIAN_EMAIL` | Librarian user email | Yes | `librarian@library.com` |
| `LIBRARIAN_PASSWORD` | Librarian password | Yes | `YourLibrarianPass123!` |
| `LIBRARIAN_FULLNAME` | Librarian full name | No | `Library Librarian` |
| `MEMBER_EMAIL` | Member user email | Yes | `member@library.com` |
| `MEMBER_PASSWORD` | Member password | Yes | `YourMemberPass123!` |
| `MEMBER_FULLNAME` | Member full name | No | `Library Member` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | No | `http://localhost:3000,http://localhost:5173` |

### 3. Start Services

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build

# Check service status
docker-compose ps

# View logs
docker-compose logs -f app
```

### 4. Access the Application

Once the application starts successfully, you can access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Health Check**: http://localhost:8080/health
- **Base API**: http://localhost:8080/api/v1

**Initial Login Credentials** (configured in `.env`):
- **Admin**: Email from `ADMIN_EMAIL`, password from `ADMIN_PASSWORD`
- **Librarian**: Email from `LIBRARIAN_EMAIL`, password from `LIBRARIAN_PASSWORD`
- **Member**: Email from `MEMBER_EMAIL`, password from `MEMBER_PASSWORD`

## Service Details

### Spring Boot Application (app)

The application automatically:
1. Waits for database to be healthy (via `depends_on` with health check)
2. Runs Hibernate schema updates (controlled by `DDL_AUTO` env var, default: `update`)
3. Seeds initial data via `DataInitializer` (permissions, roles, users, sample books)
4. Starts on port 8080 internally (exposed to host)

**Health check endpoint**: `http://localhost:8080/health` (base-path configured as `/`)

**Features**:
- JWT authentication with JJWT 0.12.3
- Token blacklist for logout
- Role-based access control (ADMIN, LIBRARIAN, MEMBER)
- Rate limiting (Bucket4j): 5 req/min auth, 100 req/min general
- Brute-force protection (Caffeine): 5 attempts, 15-min lockout
- Automatic data seeding from environment variables
- Swagger/OpenAPI documentation at `/swagger-ui.html`
- Spring Security 6.x with method-level security
- TestContainers for integration tests

### Database (PostgreSQL 16)

- **Image**: `postgres:16-alpine`
- **Port**: 5432 (internal only, not exposed to host for security)
- **Data**: Persistent Docker volume `postgres-data`
- **Health check**: `pg_isready -U postgres` command
- **Configuration** (from environment variables):
  - Database name: `POSTGRES_DB`
  - User: `POSTGRES_USER`
  - Password: `POSTGRES_PASSWORD`
- **Features**:
  - ACID compliance for transactional integrity
  - JSON/JSONB support for future extensibility
  - Full-text search capabilities
  - Connection pooling via HikariCP (max 15 connections)

## Common Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Rebuild application after code changes
docker-compose up -d --build app

# View logs
docker-compose logs -f              # All services
docker-compose logs -f app          # Application only
docker-compose logs -f db           # Database only

# Execute commands in application container
docker-compose exec app bash
docker-compose exec app ./mvnw --version

# Database access
docker-compose exec db psql -U postgres -d library_db

# Check service health
docker-compose ps

# Restart a service
docker-compose restart app

# Clean up everything (including volumes - this will DELETE all data!)
docker-compose down -v
```

## Database Management

### Development Mode

The application uses Hibernate with `ddl-auto` mode controlled by `DDL_AUTO` environment variable (default: `update`):
- Creates tables on first run based on JPA entity annotations
- Updates schema when entities change (adds new columns, tables)
- Does **NOT** drop existing data (safe for development)
- Runs `DataInitializer` to seed permissions, roles, users, and sample books

**Configurable via Environment Variable:**
```bash
# In .env file
DDL_AUTO=update  # Development: auto-update schema
# or
DDL_AUTO=validate  # Production: only validate, don't modify
```

### Production Mode (Recommended)

**Switch to Flyway for production:**

1. Set `DDL_AUTO=validate` in production `.env` file
2. Create migration scripts in `src/main/resources/db/migration/`
3. Name format: `V1__Initial_schema.sql`, `V2__Add_indexes.sql`
4. Flyway will automatically run migrations on startup
5. Example migration:
```sql
-- V1__Initial_schema.sql
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  hashed_password VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- ... more tables
```

**Note**: Flyway is already in dependencies but no migration files exist yet (relying on ddl-auto for now).

## Troubleshooting

### Application fails to start

Check logs:
```bash
docker-compose logs app
```

Common issues:
- **Database not ready**: Wait for db health check to pass (check with `docker-compose ps`)
- **Missing .env variables**: Ensure all required environment variables are set (especially `JWT_SECRET_KEY`, `*_PASSWORD`)
- **Port conflicts**: Check if port 8080 is already in use:
  ```bash
  # Windows
  netstat -an | findstr 8080
  # Linux/Mac
  lsof -i :8080
  ```
- **Build failures**: Check Maven dependencies and Java 21 compatibility
- **JWT_SECRET_KEY too short**: Must be 256-bit (64 hex characters or base64 equivalent)
- **Password validation**: Seed passwords must meet complexity requirements (8+ chars, uppercase, lowercase, digit, special char)

### Database connection issues

```bash
# Check database is running and healthy
docker-compose ps db

# Test database connection
docker-compose exec db pg_isready -U postgres

# Check application can reach database
docker-compose exec app ping db
```

### Cannot access API

1. **Check application is healthy**:
   ```bash
   docker-compose ps app
   curl http://localhost:8080/health
   ```
   Should return: `{"status":"UP"}`

2. **Check application logs**:
   ```bash
   docker-compose logs app | tail -50
   ```
   Look for "Started LibraryApiApplication" message

3. **Verify port mapping**:
   ```bash
   docker-compose ps
   ```
   Should show: `0.0.0.0:8080->8080/tcp`

### Healthcheck failures

If you see continuous healthcheck errors:

1. **Check actuator endpoint path**:
   - Endpoint should be: `http://localhost:8080/health`
   - Current config: `management.endpoints.web.base-path: /` (root path)
   - Verify actuator dependency is included in `pom.xml`

2. **Check internal port**:
   - Application runs on port 8080 internally
   - Docker exposes port 8080 to host
   - Healthcheck targets `http://localhost:8080/health` inside container

3. **Check Spring Boot startup time**:
   - Spring Boot can take 30-60 seconds to start (especially first time)
   - Docker health check has start_period of 60s to allow startup
   - Check logs: `docker-compose logs app | grep "Started LibraryApiApplication"`

### Seeding Issues

If seed users are not created:

1. **Check environment variables are loaded**:
   ```bash
   docker-compose exec app env | grep ADMIN
   docker-compose exec app env | grep JWT
   ```

2. **Check application logs during startup**:
   ```bash
   docker-compose logs app | grep -i "seed\|initial"
   ```
   Should see: "Seeding permissions", "Seeding roles", "Seeding users", "Seeding books"

3. **Verify database state**:
   ```bash
   docker-compose exec db psql -U postgres -d library_db -c "SELECT email, full_name FROM users;"
   docker-compose exec db psql -U postgres -d library_db -c "SELECT name FROM roles;"
   docker-compose exec db psql -U postgres -d library_db -c "SELECT title FROM books;"
   ```

4. **Check password validation errors**:
   - Seed passwords must meet validation requirements
   - Look for validation errors in logs
   - Ensure passwords have uppercase, lowercase, digit, and special character

### Reset everything

```bash
# Stop and remove all containers, networks, and volumes
docker-compose down -v

# Start fresh
docker-compose up --build
```

**Warning**: This will delete all database data!

## Production Deployment

For production deployment, follow these additional steps:

### 1. Secure Environment Variables

Update `.env` with strong, unique values:

```bash
# Database Configuration
POSTGRES_SERVER=your-db-host  # Or 'db' for Docker Compose
POSTGRES_PORT=5432
POSTGRES_DB=library_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<generate-strong-password-min-20-chars>

# Application Configuration
PORT=8080
SPRING_PROFILES_ACTIVE=prod
DDL_AUTO=validate  # ⚠️ IMPORTANT: Use 'validate' for production with Flyway

# JWT Configuration (256-bit / 64 hex characters)
JWT_SECRET_KEY=<generate-256-bit-hex-key>
# Generate new secret: openssl rand -hex 32
JWT_EXPIRATION=3600000  # 1 hour in milliseconds
JWT_ISSUER=library-api
JWT_AUDIENCE=library-api-client

# CORS Configuration (comma-separated allowed origins)
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com

# Initial Admin User (for seeding)
ADMIN_EMAIL=admin@yourdomain.com
ADMIN_PASSWORD=<strong-password-meeting-requirements>
ADMIN_FULLNAME=System Administrator

# Librarian User (for seeding)
LIBRARIAN_EMAIL=librarian@yourdomain.com
LIBRARIAN_PASSWORD=<strong-password-meeting-requirements>
LIBRARIAN_FULLNAME=Head Librarian

# Member User (for seeding)
MEMBER_EMAIL=member@yourdomain.com
MEMBER_PASSWORD=<strong-password-meeting-requirements>
MEMBER_FULLNAME=Library Member
```

**Password Requirements** (must meet all):
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

### 2. Database Best Practices

1. **Use migrations instead of DDL auto**:
   - Set `DDL_AUTO=validate` in production
   - Use Flyway or Liquibase for schema management

2. **Backup strategy**:
   ```bash
   # Backup database
   docker-compose exec db pg_dump -U postgres library_db > backup_$(date +%Y%m%d).sql

   # Restore database
   docker-compose exec -T db psql -U postgres library_db < backup_20260111.sql
   ```

3. **Regular backups**:
   ```bash
   # Add to crontab for daily backups
   0 2 * * * cd /path/to/project && docker-compose exec db pg_dump -U postgres library_db | gzip > /backups/library_$(date +\%Y\%m\%d).sql.gz
   ```

### 3. Security Hardening

1. **Change default seed credentials immediately after first deployment**
2. **Use HTTPS** with a reverse proxy (nginx, Apache, or cloud load balancer)
3. **Limit database port exposure** - don't expose 5432 to public internet
4. **Enable firewall rules** to restrict access to port 8080
5. **Use strong passwords** for all seed users
6. **Regular security updates** - rebuild images monthly

### 4. Monitoring and Logging

1. **Health monitoring**:
   ```bash
   # Set up health check monitoring
   */5 * * * * curl -f http://localhost:8080/health || alert-team
   ```

2. **Log aggregation**:
   ```bash
   # View logs with timestamps
   docker-compose logs -f --timestamps app
   ```

3. **Resource monitoring**:
   ```bash
   # Monitor container resources
   docker stats
   ```

### 5. Scaling Considerations

To run multiple instances behind a load balancer:

```yaml
# In docker-compose.yml
services:
  app:
    deploy:
      replicas: 3
```

Or use Docker Swarm / Kubernetes for orchestration.

## Environment Variables Reference

See the complete list of environment variables in the [Environment Configuration](#2-configure-environment) section above.

## Monitoring

### Check Service Health

```bash
# All services
docker-compose ps

# Application health endpoint
curl http://localhost:8080/health

# Database
docker-compose exec db pg_isready -U postgres
```

### Application Logs

```bash
# Follow application logs
docker-compose logs -f app

# Search logs for errors
docker-compose logs app | grep -i error

# Filter logs by timestamp
docker-compose logs app --since 30m
```

## Quick Reference

### Startup Sequence

1. Docker Compose reads `.env` file
2. PostgreSQL container starts and initializes database
3. Health check waits for database to be ready
4. Spring Boot application starts
5. Hibernate creates/updates database schema
6. DataInitializer seeds initial data (permissions, roles, users, books)
7. Application is ready to accept requests

### Port Mapping

- **Internal port**: 8080 (Spring Boot application listens inside container)
- **External port**: 8080 (exposed to host machine)
- **Access**: http://localhost:8080
- **Docker network**: Containers communicate via service names (e.g., `app` talks to `db:5432`)

### Health Check Endpoints

- **Actuator Health**: http://localhost:8080/health (configured with `base-path: /`)
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Data Persistence

All database data is stored in a Docker volume named `postgres-data`. This persists across container restarts but is deleted when running `docker-compose down -v`.

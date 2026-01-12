# Library Management API

A RESTful API for managing a library system built with Spring Boot 4.0.1, Java 21, and PostgreSQL 16.

## Quick Start

### Development

```bash
# 1. Copy environment file
cp .env.example .env

# 2. Edit .env and set your credentials (minimum required):
#    - ADMIN_PASSWORD
#    - LIBRARIAN_PASSWORD
#    - MEMBER_PASSWORD
#    - JWT_SECRET_KEY (generate with: openssl rand -hex 32)

# 3. Run with Docker Compose
docker-compose up

# 4. Access the API
# - API Base: http://localhost:8080/api/v1
# - Swagger UI: http://localhost:8080/swagger-ui.html
# - Health Check: http://localhost:8080/health
```

### Production Deployment

For production deployment instructions, see [Deploy.md](Deploy.md).

## Features

- **JWT Authentication** - Secure token-based authentication with logout support
- **Role-Based Access Control** - Three roles: ADMIN, LIBRARIAN, MEMBER
- **Rate Limiting** - Bucket4j token bucket (5 req/min auth, 100 req/min general)
- **Brute-Force Protection** - Account lockout after 5 failed login attempts
- **User Management** - ADMIN/LIBRARIAN can register new borrowers
- **Book Management** - CRUD operations for library books
- **Borrow System** - Borrow and return books with tracking
- **API Documentation** - Interactive Swagger UI with OpenAPI 3.0

## Tech Stack

- **Spring Boot 4.0.1** - Enterprise Java framework
- **Java 21** - Latest LTS version
- **PostgreSQL 16** - Relational database
- **Spring Security 6.x** - Authentication & authorization
- **JJWT 0.12.3** - JWT token handling
- **Bucket4j** - Rate limiting
- **Hibernate 6.x** - ORM with JPA 3.1
- **Docker Compose** - Container orchestration
- **TestContainers** - Integration testing

## API Endpoints

### Authentication
- `POST /api/v1/auth/access-token` - Login
- `POST /api/v1/auth/logout` - Logout

### User Management (ADMIN/LIBRARIAN)
- `POST /api/v1/users` - Register new borrower

### Books (LIBRARIAN: write, ALL: read)
- `POST /api/v1/books` - Create book
- `GET /api/v1/books` - List books
- `GET /api/v1/books/{id}` - Get book
- `PATCH /api/v1/books/{id}` - Update book
- `DELETE /api/v1/books/{id}` - Delete book

### Borrows (MEMBER: own records, LIBRARIAN: all records)
- `POST /api/v1/borrows` - Borrow book
- `POST /api/v1/borrows/{id}/return` - Return book
- `GET /api/v1/borrows/me` - My borrow records
- `GET /api/v1/borrows` - All borrow records (LIBRARIAN)

For complete API documentation with examples, see [API.md](API.md).

## Default Users

Three users are seeded on first startup (configure in `.env`):

| Role | Email (env) | Password (env) |
|------|-------------|----------------|
| Admin | `ADMIN_EMAIL` | `ADMIN_PASSWORD` |
| Librarian | `LIBRARIAN_EMAIL` | `LIBRARIAN_PASSWORD` |
| Member | `MEMBER_EMAIL` | `MEMBER_PASSWORD` |

## Documentation

- **[API.md](API.md)** - Complete API reference with examples
- **[Deploy.md](Deploy.md)** - Production deployment guide
- **[Assumptions.md](Assumptions.md)** - Design decisions and assumptions

## Project Structure

```
src/main/java/com/saufi/library_api/
├── controller/          # REST endpoints
├── service/             # Business logic
├── repository/          # Database access
├── domain/
│   ├── entity/          # JPA entities
│   └── enums/           # Enums (Roles, Permissions)
├── dto/                 # Request/Response DTOs
├── security/            # JWT, Rate limiting, Security config
├── validation/          # Custom validators
└── config/              # Application configuration
```

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test
./mvnw test -Dtest=BookControllerIT

# Run with coverage
./mvnw test jacoco:report
```

## Environment Variables

Key variables (see `.env.example` for full list):

```bash
# Database
POSTGRES_SERVER=db
POSTGRES_DB=library_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password_here

# Application
PORT=8080
DDL_AUTO=update  # Use 'validate' in production

# JWT (256-bit hex key)
JWT_SECRET_KEY=your-256-bit-secret-key
JWT_EXPIRATION=3600000

# Seed Users
ADMIN_EMAIL=admin@library.com
ADMIN_PASSWORD=ChangeThisPassword123!
LIBRARIAN_EMAIL=librarian@library.com
LIBRARIAN_PASSWORD=LibrarianPass123!
MEMBER_EMAIL=member@library.com
MEMBER_PASSWORD=MemberPass123!
```

## License

This project is for educational purposes.

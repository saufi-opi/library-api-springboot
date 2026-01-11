# Library Management API - Complete API Documentation

This document provides comprehensive documentation for all API endpoints in the Library Management System.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Pagination and Filtering](#pagination-and-filtering)
- [API Endpoints](#api-endpoints)
  - [Authentication Endpoints](#authentication-endpoints)
  - [User Management](#user-management)
  - [Book Management](#book-management)
  - [Borrow Management](#borrow-management)

---

## Overview

**Base URL:** `http://localhost:8080/api/v1`

**API Version:** v1

**Protocol:** HTTP/HTTPS

**Data Format:** JSON

**Character Encoding:** UTF-8

---

## Authentication

This API uses **JWT (JSON Web Token)** bearer authentication powered by Spring Security.

### Getting a Token

**Endpoint:** `POST /auth/access-token`

**Request:**
```http
POST /api/v1/auth/access-token HTTP/1.1
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "bearer",
  "expiresIn": 3600
}
```

### Using the Token

Include the token in the `Authorization` header for all authenticated requests:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Expiration

- Access tokens expire after **1 hour** (3600 seconds, configurable via `JWT_EXPIRATION` environment variable)
- The `expiresIn` field in the response indicates seconds until expiration
- When a token expires, you'll receive a `401 Unauthorized` response
- Request a new token using the login endpoint

### Logout

**Endpoint:** `POST /auth/logout`

To revoke a token before expiration, call the logout endpoint. The token will be blacklisted and cannot be used for further requests.

### Default Test Users

The application seeds three default users on first startup (credentials configurable via environment variables):

| Role | Email | Password (env variable) |
|------|-------|-------------------|
| Admin | From `ADMIN_EMAIL` | From `ADMIN_PASSWORD` |
| Librarian | From `LIBRARIAN_EMAIL` | From `LIBRARIAN_PASSWORD` |
| Member | From `MEMBER_EMAIL` | From `MEMBER_PASSWORD` |

**Note:** Set these credentials in your `.env` file or environment variables before starting the application.

---

## Error Handling

### Standard Error Response

All errors follow a consistent format (handled by Spring Boot's `@RestControllerAdvice` global exception handler):

```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Error message describing what went wrong",
  "path": "/api/v1/books/123"
}
```

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request succeeded |
| 400 | Bad Request | Invalid input data or business logic violation |
| 401 | Unauthorized | Missing or invalid authentication token |
| 403 | Forbidden | Insufficient permissions for this action |
| 404 | Not Found | Requested resource doesn't exist |
| 409 | Conflict | Resource conflict (e.g., duplicate email) |
| 422 | Unprocessable Entity | Validation error in request data |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server-side error |

---

## Rate Limiting

**Implementation:** Bucket4j token bucket algorithm with in-memory bucket cache per IP address.

**Default Limits (per IP address):**
- Authentication endpoints (`/api/v1/auth/*`): **5 requests per 1 minute** (configurable via `application.yaml`)
- General endpoints: **100 requests per 1 minute** (configurable via `application.yaml`)

**Rate Limit Response:**
When rate-limited, you'll receive a `429 Too Many Requests` response with a `Retry-After` header. Wait before retrying.

**Configuration:**
Rate limits can be adjusted in `application.yaml` under `application.security.rate-limit`:
```yaml
application:
  security:
    rate-limit:
      auth-endpoints:
        capacity: 5
        refill-tokens: 5
        refill-duration-minutes: 1
      general-endpoints:
        capacity: 100
        refill-tokens: 100
        refill-duration-minutes: 1
```

---

## Pagination and Filtering

### Pagination Parameters

List endpoints support pagination:

| Parameter | Type | Default | Max | Description |
|-----------|------|---------|-----|-------------|
| `skip` | integer | 0 | - | Number of records to skip |
| `limit` | integer | 100 | 1000 | Maximum number of records to return |

**Example:**
```http
GET /api/v1/books/?skip=20&limit=10
```

### Sorting

Use the `sort` parameter with field names. Prefix with `-` for descending order:

| Value | Description |
|-------|-------------|
| `title` | Sort by title (A-Z) |
| `-title` | Sort by title (Z-A) |
| `created_at` | Sort by creation date (oldest first) |
| `-created_at` | Sort by creation date (newest first) |

**Example:**
```http
GET /api/v1/books/?sort=-created_at
```

---

## API Endpoints

---

## Authentication Endpoints

### 1. Login

Get an access token for authentication.

**Endpoint:** `POST /auth/access-token`

**Rate Limit:** 5 requests per 60 seconds per IP

**Authentication:** None (public endpoint)

**Brute-Force Protection:** After 5 failed login attempts, account is locked for 15 minutes

**Request Body (JSON):**
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**cURL Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "yourpassword"
  }'
```

**Success Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiZXhwIjoxNzAyMzE4ODAwfQ.xyz",
  "tokenType": "bearer",
  "expiresIn": 3600
}
```

**Error Responses:**

- **401 Unauthorized** - Invalid credentials:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

- **403 Forbidden** - Account locked due to too many failed attempts:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Account locked due to too many failed login attempts. Try again in 15 minutes."
}
```

---

### 2. Logout

Revoke the current access token by adding it to the blacklist.

**Endpoint:** `POST /auth/logout`

**Rate Limit:** 5 requests per 60 seconds per IP

**Authentication:** Required

**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/auth/logout" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```
(Empty response body with 200 status)
```

**Error Response:**

- **401 Unauthorized** - Invalid or missing token

**Note:** After logout, the token cannot be used for further requests even if it hasn't expired yet.

---

## Book Management

### 3. Register a New Book

Add a new book to the library. Creates a new book record even if the ISBN already exists (allows multiple copies).

**Endpoint:** `POST /books/`

**Authentication:** Required

**Permission:** `books:create` (Librarian role by default)

**Request Body (JSON):**
```json
{
  "isbn": "978-0-13-468599-1",
  "title": "The Pragmatic Programmer",
  "author": "David Thomas, Andrew Hunt"
}
```

**Field Requirements:**
- `isbn` (required): ISBN-10 or ISBN-13 format (validated with custom `@ValidISBN` annotation)
  - ISBN-10: 10 digits (last digit can be X)
  - ISBN-13: 13 digits
  - Hyphens and spaces are allowed and will be removed during normalization
  - Maximum 20 characters before formatting
- `title` (required): 1-500 characters (validated with `@Size`)
- `author` (required): 1-255 characters (validated with `@Size`)

**ISBN Consistency Rule:**
- If another book with the same ISBN exists, the new book MUST have the same title and author
- This ensures ISBN integrity across the system
- Enforced by business logic validation in the service layer

**cURL Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/books/" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0-13-468599-1",
    "title": "The Pragmatic Programmer",
    "author": "David Thomas, Andrew Hunt"
  }'
```

**Success Response (201 Created):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "isbn": "9780134685991",
  "title": "The Pragmatic Programmer",
  "author": "David Thomas, Andrew Hunt",
  "isAvailable": true,
  "createdAt": "2024-12-10T10:30:00Z"
}
```

**Note:** The ISBN is normalized (hyphens/spaces removed) in the response.

**Error Responses:**

- **403 Forbidden** - Insufficient permissions:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

- **400 Bad Request** - ISBN inconsistency:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "A book with ISBN 9780134685991 already exists with different title/author"
}
```

- **400 Bad Request** - Validation errors (Bean Validation):
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": {
    "isbn": "ISBN is required",
    "title": "Title is required"
  }
}
```

---

### 4. List All Books

Get a paginated list of all books in the library with optional filters.

**Endpoint:** `GET /books/`

**Authentication:** Required

**Permission:** `books:read` (Member and Librarian roles)

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `skip` | integer | 0 | Number of records to skip |
| `limit` | integer | 100 | Max records to return (max: 1000) |
| `search` | string | - | Search in title and author (case-insensitive) |
| `isbn` | string | - | Filter by exact ISBN |
| `available_only` | boolean | false | Show only available books |
| `sort` | string | - | Sort by: `title`, `author`, `created_at` (prefix `-` for descending) |

**cURL Examples:**

Get all books:
```bash
curl -X GET "http://localhost:8080/api/v1/books/" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Get only available books:
```bash
curl -X GET "http://localhost:8080/api/v1/books/?available_only=true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Search for books by title or author:
```bash
curl -X GET "http://localhost:8080/api/v1/books/?search=python" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Get books by ISBN:
```bash
curl -X GET "http://localhost:8080/api/v1/books/?isbn=9780134685991" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```json
{
  "data": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "isbn": "9780134685991",
      "title": "The Pragmatic Programmer",
      "author": "David Thomas, Andrew Hunt",
      "isAvailable": true,
      "createdAt": "2024-12-10T10:30:00Z"
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "isbn": "9780134685991",
      "title": "The Pragmatic Programmer",
      "author": "David Thomas, Andrew Hunt",
      "isAvailable": false,
      "createdAt": "2024-12-10T11:00:00Z"
    }
  ],
  "total": 2,
  "skip": 0,
  "limit": 100
}
```

**Note:** Multiple books can have the same ISBN (multiple copies).

---

### 5. Get Book by ID

Get details of a specific book.

**Endpoint:** `GET /books/{book_id}`

**Authentication:** Required

**Permission:** `books:read`

**Path Parameters:**
- `book_id` (UUID): The unique identifier of the book

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/books/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "isbn": "9780134685991",
  "title": "The Pragmatic Programmer",
  "author": "David Thomas, Andrew Hunt",
  "isAvailable": true,
  "createdAt": "2024-12-10T10:30:00Z"
}
```

**Error Response:**

- **404 Not Found** - Book doesn't exist:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found"
}
```

---

### 6. Update Book

Update book details (ISBN, title, or author).

**Endpoint:** `PATCH /books/{book_id}`

**Authentication:** Required

**Permission:** `books:update` (Librarian role by default)

**Path Parameters:**
- `book_id` (UUID): The unique identifier of the book

**Request Body (JSON):**
```json
{
  "title": "The Pragmatic Programmer: 20th Anniversary Edition",
  "author": "David Thomas, Andrew Hunt"
}
```

**Note:** All fields are optional. Only include fields you want to update.

**cURL Example:**
```bash
curl -X PATCH "http://localhost:8080/api/v1/books/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Pragmatic Programmer: 20th Anniversary Edition"
  }'
```

**Success Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "isbn": "9780134685991",
  "title": "The Pragmatic Programmer: 20th Anniversary Edition",
  "author": "David Thomas, Andrew Hunt",
  "isAvailable": true,
  "createdAt": "2024-12-10T10:30:00Z"
}
```

**Error Response:**

- **404 Not Found** - Book doesn't exist

---

### 7. Delete Book

Remove a book from the library.

**Endpoint:** `DELETE /books/{book_id}`

**Authentication:** Required

**Permission:** `books:delete` (Librarian role by default)

**Path Parameters:**
- `book_id` (UUID): The unique identifier of the book

**cURL Example:**
```bash
curl -X DELETE "http://localhost:8080/api/v1/books/123e4567-e89b-12d3-a456-426614174000" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (204 No Content):**
```
(Empty response body)
```

**Error Response:**

- **404 Not Found** - Book doesn't exist

---

## Borrow Management

### 8. Borrow a Book

Borrow an available book on behalf of the current user.

**Endpoint:** `POST /borrows/`

**Authentication:** Required

**Permission:** `borrows:create` (Member and Librarian roles)

**Request Body (JSON):**
```json
{
  "bookId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Business Rules:**
- Book must exist
- Book must be available (`is_available` = true)
- One book can only be borrowed by one user at a time
- User borrows the book for themselves (cannot borrow on behalf of others)

**cURL Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/borrows/" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Success Response (201 Created):**
```json
{
  "id": "987e6543-e89b-12d3-a456-426614174999",
  "bookId": "123e4567-e89b-12d3-a456-426614174000",
  "borrowerId": "456e7890-e89b-12d3-a456-426614175555",
  "borrowedAt": "2024-12-10T14:30:00Z",
  "returnedAt": null
}
```

**Error Responses:**

- **404 Not Found** - Book doesn't exist:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book not found"
}
```

- **400 Bad Request** - Book not available:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Book is not available for borrowing"
}
```

---

### 9. Return a Borrowed Book

Return a book that was previously borrowed.

**Endpoint:** `POST /borrows/{recordId}/return`

**Authentication:** Required

**Permission:** `borrows:return` (Member and Librarian roles)

**Path Parameters:**
- `borrow_id` (UUID): The unique identifier of the borrow record

**Business Rules:**
- Borrow record must exist
- Book must not be already returned
- Only the borrower can return their own book

**cURL Example:**
```bash
curl -X POST "http://localhost:8080/api/v1/borrows/987e6543-e89b-12d3-a456-426614174999/return" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```json
{
  "id": "987e6543-e89b-12d3-a456-426614174999",
  "bookId": "123e4567-e89b-12d3-a456-426614174000",
  "borrowerId": "456e7890-e89b-12d3-a456-426614175555",
  "borrowedAt": "2024-12-10T14:30:00Z",
  "returnedAt": "2024-12-10T16:45:00Z"
}
```

**Error Responses:**

- **404 Not Found** - Borrow record doesn't exist:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Borrow record not found"
}
```

- **400 Bad Request** - Book already returned:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Book has already been returned"
}
```

- **403 Forbidden** - Not the borrower:
```json
{
  "timestamp": "2026-01-11T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You can only return books you borrowed"
}
```

---

### 10. List My Borrow Records

Get a list of borrow records for the current user.

**Endpoint:** `GET /borrows/me`

**Authentication:** Required

**Permission:** `borrows:read` (Member and Librarian roles)

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `skip` | integer | 0 | Number of records to skip |
| `limit` | integer | 100 | Max records to return (max: 1000) |
| `activeOnly` | boolean | false | Show only unreturned books |
| `bookId` | UUID | - | Filter by specific book |
| `sort` | string | - | Sort by: `borrowedAt`, `returnedAt` (prefix `-` for descending) |

**cURL Examples:**

Get all my borrows:
```bash
curl -X GET "http://localhost:8080/api/v1/borrows/me" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Get only active (unreturned) borrows:
```bash
curl -X GET "http://localhost:8080/api/v1/borrows/me?activeOnly=true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```json
{
  "data": [
    {
      "id": "987e6543-e89b-12d3-a456-426614174999",
      "bookId": "123e4567-e89b-12d3-a456-426614174000",
      "borrowerId": "456e7890-e89b-12d3-a456-426614175555",
      "borrowedAt": "2024-12-10T14:30:00Z",
      "returnedAt": null
    }
  ],
  "total": 1,
  "skip": 0,
  "limit": 100
}
```

---

### 11. List All Borrow Records (Librarian)

Get a list of all borrow records in the system (librarian only).

**Endpoint:** `GET /borrows/`

**Authentication:** Required

**Permission:** `borrows:read_all` (Librarian role by default)

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `skip` | integer | 0 | Number of records to skip |
| `limit` | integer | 100 | Max records to return (max: 1000) |
| `activeOnly` | boolean | false | Show only unreturned books |
| `bookId` | UUID | - | Filter by specific book |
| `borrowerId` | UUID | - | Filter by specific borrower |
| `sort` | string | - | Sort by: `borrowedAt`, `returnedAt` (prefix `-` for descending) |

**cURL Example:**
```bash
curl -X GET "http://localhost:8080/api/v1/borrows/?activeOnly=true" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Success Response (200 OK):**
```json
{
  "data": [
    {
      "id": "987e6543-e89b-12d3-a456-426614174999",
      "bookId": "123e4567-e89b-12d3-a456-426614174000",
      "borrowerId": "456e7890-e89b-12d3-a456-426614175555",
      "borrowedAt": "2024-12-10T14:30:00Z",
      "returnedAt": null
    }
  ],
  "total": 1,
  "skip": 0,
  "limit": 100
}
```

---

## Complete User Journeys

### Journey 1: Member Logs in and Borrows a Book

```bash
# Step 1: Login to get access token (use seeded member account)
TOKEN=$(curl -X POST "http://localhost:8080/api/v1/auth/access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "member@example.com",
    "password": "your_member_password"
  }' | jq -r '.accessToken')

# Step 2: List available books
curl -X GET "http://localhost:8080/api/v1/books/?availableOnly=true" \
  -H "Authorization: Bearer $TOKEN"

# Step 3: Borrow a book (use book ID from previous step)
curl -X POST "http://localhost:8080/api/v1/borrows/" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookId": "123e4567-e89b-12d3-a456-426614174000"
  }'

# Step 4: View my active borrows
curl -X GET "http://localhost:8080/api/v1/borrows/me?activeOnly=true" \
  -H "Authorization: Bearer $TOKEN"

# Step 5: Return the book (use record ID from step 3)
curl -X POST "http://localhost:8080/api/v1/borrows/987e6543-e89b-12d3-a456-426614174999/return" \
  -H "Authorization: Bearer $TOKEN"

# Step 6: Logout (optional - revokes the token)
curl -X POST "http://localhost:8080/api/v1/auth/logout" \
  -H "Authorization: Bearer $TOKEN"
```

### Journey 2: Librarian Manages Books

```bash
# Step 1: Login as librarian
TOKEN=$(curl -X POST "http://localhost:8080/api/v1/auth/access-token" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "librarian@example.com",
    "password": "your_librarian_password"
  }' | jq -r '.accessToken')

# Step 2: Register a new book
BOOK_ID=$(curl -X POST "http://localhost:8080/api/v1/books/" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0-13-468599-1",
    "title": "The Pragmatic Programmer",
    "author": "David Thomas, Andrew Hunt"
  }' | jq -r '.id')

# Step 3: Register another copy of the same book
curl -X POST "http://localhost:8080/api/v1/books/" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0-13-468599-1",
    "title": "The Pragmatic Programmer",
    "author": "David Thomas, Andrew Hunt"
  }'

# Step 4: View all books with this ISBN
curl -X GET "http://localhost:8080/api/v1/books/?isbn=9780134685991" \
  -H "Authorization: Bearer $TOKEN"

# Step 5: View all active borrows in the system
curl -X GET "http://localhost:8080/api/v1/borrows/?activeOnly=true" \
  -H "Authorization: Bearer $TOKEN"

# Step 6: Update book details
curl -X PATCH "http://localhost:8080/api/v1/books/$BOOK_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Pragmatic Programmer: 20th Anniversary Edition"
  }'
```

---

## Interactive API Documentation

For interactive API testing and monitoring, visit:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
  - Interactive API documentation powered by SpringDoc OpenAPI
  - Test all endpoints directly from the browser
  - Automatic schema generation from code annotations
  - Built-in authentication support

- **API Docs (JSON):** http://localhost:8080/v3/api-docs
  - OpenAPI 3.0 specification in JSON format
  - Can be imported into Postman, Insomnia, or other API clients

- **Actuator Health:** http://localhost:8080/health
  - Health check endpoint for monitoring
  - Returns `{"status":"UP"}` when application is healthy

**Swagger UI Features:**
- Complete API schema with all endpoints
- Request/response examples for each endpoint
- Try-it-out functionality with live API calls
- JWT Bearer token authentication support
- Automatic validation of request/response schemas
- Filter and search capabilities
- Alphabetically sorted tags and operations

---

## Additional Resources

- [Assumptions.md](Assumptions.md) - Complete list of assumptions and design decisions
- [Deploy.md](Deploy.md) - Deployment guide with Docker Compose
- [CLAUDE.md](CLAUDE.md) - Developer guide for working with this codebase
- Application built with:
  - **Spring Boot 4.0.1** - Enterprise Java framework
  - **Java 21** - Latest LTS Java version
  - **Spring Security 6.x** - Authentication and authorization
  - **Spring Data JPA** - Database access with Hibernate
  - **PostgreSQL 16** - Relational database
  - **Bucket4j** - Rate limiting with token bucket algorithm
  - **JJWT 0.12.3** - JWT token generation and validation
  - **SpringDoc OpenAPI** - API documentation generation
  - **Lombok** - Boilerplate code reduction
  - **Maven** - Build and dependency management
  - **TestContainers** - Integration testing with PostgreSQL

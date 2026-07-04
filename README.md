# Inventory & Order Management System

A production-ready REST API backend for managing inventory and orders, built with Java 17 and Spring Boot 3.2.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Auth-red?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

## Performance
> Load tested with Postman — **9,025 requests** handled with **zero errors**

| Metric | Result |
|---|---|
| Requests/second | 126 req/sec |
| Avg response time | 70ms |
| P90 | 180ms |
| P99 | 510ms |
| Error rate | 0.00% |

## Features

- **JWT Authentication** — Stateless auth with access + refresh tokens
- **Role-Based Access Control** — ADMIN / CUSTOMER / WAREHOUSE_STAFF
- **Product Management** — CRUD, category filtering, SKU lookup, soft delete
- **Order Management** — Full lifecycle: PENDING → CONFIRMED → SHIPPED → DELIVERED
- **Inventory Control** — Real-time stock updates, movement history, low-stock alerts
- **Swagger UI** — Interactive API docs at `/swagger-ui.html`
- **Global Exception Handling** — Consistent JSON error responses
- **Docker Support** — Runs with a single command

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Database | PostgreSQL 15 |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security 6 + JWT (JJWT 0.12.3) |
| Documentation | Springdoc OpenAPI 2.1.0 (Swagger) |
| Build | Maven 3.9+ |
| Containerization | Docker + Docker Compose |

## Quick Start

### Option 1 — Docker (Recommended)
```bash
git clone https://github.com/SKKammar/inventory.git
cd inventory
docker-compose up --build
```
App starts at `http://localhost:8080/swagger-ui.html` — no PostgreSQL setup needed.

### Option 2 — Local
```bash
# 1. Update .env with your local or remote PostgreSQL credentials

# 2. Run
mvn spring-boot:run
```

## Default Credentials
```
Username: admin
Password: Admin@123
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/login` | Login and get JWT token |
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/refresh-token` | Refresh access token |
| POST | `/api/auth/logout` | Invalidate refresh token |

### Products
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/products` | Authenticated |
| POST | `/api/products` | ADMIN only |
| PUT | `/api/products/{id}` | ADMIN only |
| DELETE | `/api/products/{id}` | ADMIN only |
| GET | `/api/products/stock/low` | ADMIN / WAREHOUSE |

### Orders
| Method | Endpoint | Auth |
|---|---|---|
| POST | `/api/orders` | Authenticated |
| GET | `/api/orders` | ADMIN only |
| PUT | `/api/orders/{id}/status` | ADMIN / WAREHOUSE |
| GET | `/api/orders/stats/all` | ADMIN only |

### Inventory
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/inventory/stock/{productId}` | ADMIN / WAREHOUSE |
| GET | `/api/inventory/history/{productId}` | ADMIN / WAREHOUSE |
| POST | `/api/inventory/adjustment` | ADMIN only |

## Project Structure
```
src/main/java/com/example/inventory/
├── config/          # Security, OpenAPI, Exception Handler
├── controller/      # REST Controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA Entities
├── exception/       # Custom Exceptions
├── init/            # Data Seeder
├── repository/      # Spring Data JPA Repositories
├── security/        # JWT Filter, Token Provider
└── service/         # Business Logic
```

## Sample Request

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'
```

### Create Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro",
    "sku": "MBP-001",
    "category": "Electronics",
    "price": 1299.99,
    "currentStock": 25,
    "minStockLevel": 5,
    "unit": "PCS"
  }'
```

## Security Notes

### Authentication Architecture
JWT tokens are stored in **httpOnly, SameSite=Lax cookies** — not in localStorage.

| Property | Value | Why |
|----------|-------|-----|
| `httpOnly` | `true` | Prevents JavaScript access → mitigates XSS token theft |
| `SameSite` | `Lax` | Browser won't send cookie on cross-origin POST → first CSRF defense layer |
| `Secure` | `true` (prod) | Requires HTTPS → prevents token interception over HTTP |

### CSRF Protection
**Strategy: CSRF token validation (defense-in-depth)**

The backend uses Spring Security's `CookieCsrfTokenRepository` which:
1. Sets a non-httpOnly `XSRF-TOKEN` cookie on every response
2. The frontend reads this cookie and sends its value as the `X-XSRF-TOKEN` header
3. Spring validates that the header matches the cookie
4. Attackers on different origins can't read our cookies (same-origin policy) → can't forge the header

**Why not SameSite=Lax alone?** SameSite=Lax blocks cross-origin form POST but doesn't cover all attack vectors (subdomain attacks, browser quirks). CSRF tokens provide a second independent layer of protection.

**CSRF-exempt endpoints**: `/api/auth/login`, `/api/auth/register` (no session to hijack at login time).

### Bearer Header Fallback
The JWT filter supports `Authorization: Bearer` headers **only in dev/test profiles** for Postman and Swagger convenience. This is **disabled in production** (`app.auth.allow-bearer-header: false`) to prevent bypassing cookie-based CSRF protections.

### CORS
Configured with `allowCredentials: true` and explicit origin whitelist. Wildcard origins are not allowed when credentials are enabled (browser security requirement).

## Author
**Santosh K Kammar**
- GitHub: [@SKKammar](https://github.com/SKKammar)

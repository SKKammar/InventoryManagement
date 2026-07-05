# Inventory & Order Management System

A REST API backend for managing inventory and orders, built with Java 17 and Spring Boot 3.2.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Auth-red?style=flat-square)
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

- **JWT Cookie Authentication** — httpOnly cookie-based auth with access + refresh tokens (login, register, logout, session check, forgot-password, reset-password)
- **Role-Based Access Control** — Three roles: `ADMIN`, `CUSTOMER`, `WAREHOUSE_STAFF`
- **Product Management** — Full CRUD with soft-delete; writes restricted to ADMIN
- **Order Management** — Order creation (any authenticated user), all-orders listing (ADMIN / WAREHOUSE_STAFF), own-orders listing (any authenticated user)
- **Swagger UI** — Interactive API docs at `/swagger-ui.html`
- **Global Exception Handling** — Consistent JSON error responses via `@RestControllerAdvice`

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.0 |
| Database | PostgreSQL |
| ORM | Hibernate / Spring Data JPA |
| Security | Spring Security 6 + JWT (JJWT 0.12.3) |
| Documentation | Springdoc OpenAPI 2.3.0 (Swagger) |
| Mapping | MapStruct 1.5.5.Final |
| Build | Maven |

## Quick Start

### Prerequisites
- Java 17
- Maven 3.9+
- PostgreSQL instance (local or remote, e.g. Supabase)

### Setup
```bash
git clone https://github.com/SKKammar/InventoryManagement.git
cd InventoryManagement

# 1. Copy .env.example and fill in your Postgres credentials and a JWT secret
cp .env.example .env

# 2. Export the environment variables (or set them in your IDE run config)
#    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/inventory_db
#    SPRING_DATASOURCE_USERNAME=your_db_username
#    SPRING_DATASOURCE_PASSWORD=your_db_password
#    APP_JWT_SECRET=your_jwt_secret_at_least_64_characters_long

# 3. Run
mvn spring-boot:run
```
App starts at `http://localhost:8080/swagger-ui.html`.

## Default Credentials

Seeded automatically on first run when the `users` table is empty:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | CUSTOMER |

## API Endpoints

### Authentication (`/api/auth`)
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Login — sets JWT cookies | Public |
| POST | `/api/auth/register` | Register new user (CUSTOMER role) — sets JWT cookies | Public |
| POST | `/api/auth/logout` | Logout — clears JWT cookies | Public |
| GET | `/api/auth/me` | Get current authenticated user | Authenticated |
| POST | `/api/auth/refresh-token` | Refresh access token via refresh cookie | Public |
| POST | `/api/auth/forgot-password` | Request password reset (logs mock email to console) | Public |
| POST | `/api/auth/reset-password` | Reset password with token | Public |

### Products (`/api/products`)
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/products` | Authenticated |
| GET | `/api/products/{id}` | Authenticated |
| POST | `/api/products` | ADMIN only |
| PUT | `/api/products/{id}` | ADMIN only |
| DELETE | `/api/products/{id}` | ADMIN only (soft-delete) |

### Orders (`/api/orders`)
| Method | Endpoint | Auth |
|---|---|---|
| GET | `/api/orders` | ADMIN / WAREHOUSE_STAFF |
| GET | `/api/orders/my` | Authenticated (returns own orders only) |
| POST | `/api/orders` | Authenticated |

## Project Structure
```
src/main/java/com/example/inventory/
├── config/          # DataSeeder
├── controller/      # AuthController, ProductController, OrderController
├── dto/             # Request/Response DTOs
├── entity/          # JPA Entities (User, Product, Order, OrderItem)
├── enums/           # RoleType, OrderStatus
├── exception/       # GlobalExceptionHandler
├── mapper/          # MapStruct mappers (OrderMapper, ProductMapper)
├── repository/      # Spring Data JPA Repositories
├── security/        # SecurityConfig, JwtUtil, JwtAuthenticationFilter, UserDetailsServiceImpl
└── service/         # OrderService, ProductService
```

## Security Notes

### Cookie-Based JWT Authentication
JWT tokens are stored in **httpOnly cookies**, not in localStorage or headers.

| Property | Value | Notes |
|----------|-------|-------|
| `httpOnly` | `true` | Prevents JavaScript access — mitigates XSS token theft |
| `SameSite` | `Lax` | Browser won't send cookie on cross-origin POST |
| `Secure` | `true` | Requires HTTPS — tokens won't be sent over plain HTTP |

The JWT filter reads **only** from the `accessToken` cookie. There is no `Authorization: Bearer` header fallback.

### CSRF Protection
**CSRF is currently disabled** (`csrf(AbstractHttpConfigurer::disable)` in `SecurityConfig`). This is a known limitation. The `SameSite=Lax` cookie attribute provides partial mitigation against cross-origin POST attacks, but a full CSRF token mechanism is not implemented.

### CORS
Configured with `allowCredentials: true` and an explicit origin whitelist (`localhost:3000`, `localhost:3001`). Wildcard origins are not allowed.

## Future Work (Not Yet Implemented)

The following features are **not currently in the codebase** and are flagged here for future consideration:

- Order status update endpoint (`PUT /api/orders/{id}/status`)
- Order statistics/analytics endpoint
- Inventory adjustment history tracking
- Low-stock alert endpoint
- Email integration for password reset (currently logs to console)
- CSRF token protection (defense-in-depth beyond `SameSite=Lax`)

## Author
**Santosh K Kammar**
- GitHub: [@SKKammar](https://github.com/SKKammar)



```markdown
# Ecommerce Backend

A Spring Boot REST API backend for an e-commerce platform, built as a portfolio/learning project. Covers product catalog management, JWT-based authentication with role-based access control, a persistent server-side cart, order processing with race-condition-safe stock handling, address management, and payment integration via Paystack.

## Tech Stack

- **Java 21**, **Spring Boot 4.1.0**
- **PostgreSQL 18** — database
- **Spring Data JPA / Hibernate** — ORM
- **Spring Security + JWT** — authentication and authorization
- **Paystack** — payment processing (test mode)
- **springdoc-openapi (Swagger UI)** — API documentation
- **JUnit 5 + Mockito** — unit testing
- **Maven** — build tool
- **Lombok** — boilerplate reduction

## Features

- **Product catalog** — CRUD, search, category filtering, pagination
- **Categories** — CRUD
- **Authentication** — register, login, JWT issuance, forgot/reset password
- **Role-based access control** — `CUSTOMER` and `ADMIN` roles; product/category writes and order status updates are admin-only
- **Cart** — persistent, server-side, tied to the logged-in user; add/update/remove items, auto-merges duplicate product entries
- **Orders** — two ways to place an order: manually specifying items, or checking out directly from the cart. Both share the same atomic, race-condition-safe stock decrement (prevents overselling under concurrent requests), server-computed totals (never trusted from the client), and address snapshotting at checkout time
- **Addresses** — saved shipping addresses per user
- **Payments** — Paystack integration: transaction initialization, webhook handling with HMAC signature verification and idempotency protection against duplicate events
- **Testing** — unit test coverage on core business logic (stock decrement, order creation, rollback behavior)

## Getting Started

### Prerequisites
- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 18 running locally
- A [Paystack](https://paystack.com) account (free, test mode) for payment features

### Setup

1. **Clone the repo**
   ```
git clone https://github.com/your-username/ecommerce-backend.git
cd ecommerce-backend
   ```

2. **Create the database** (via `psql` or pgAdmin):
   ```sql
   CREATE DATABASE ecommerce_db;
   ```

3. **Configure your environment**
   Copy the example config and fill in your real values:
   ```
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```
   Then edit `application.yml` with:
   - Your Postgres password
   - A JWT secret (any long random string, 32+ characters)
   - Your Paystack **test** secret key (from Paystack dashboard → Settings → API Keys & Webhooks)

4. **Build and run**
   ```
   mvnw clean install
   mvnw spring-boot:run
   ```

5. **Explore the API**
   Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## API Overview

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/forgot-password` | Public |
| POST | `/api/auth/reset-password` | Public |

### Products & Categories
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| POST/PUT/DELETE | `/api/products/**` | Admin |
| GET | `/api/categories` | Public |
| POST | `/api/categories` | Admin |

### Cart
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/cart` | Authenticated |
| POST | `/api/cart/items` | Authenticated |
| PUT | `/api/cart/items/{itemId}` | Authenticated (owner only) |
| DELETE | `/api/cart/items/{itemId}` | Authenticated (owner only) |
| DELETE | `/api/cart` | Authenticated |

### Orders
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/orders` | Authenticated — place an order with explicit items |
| POST | `/api/orders/checkout` | Authenticated — place an order from the current cart, then clears it |
| GET | `/api/orders/{id}` | Authenticated |
| GET | `/api/orders` (my orders) | Authenticated |
| PUT | `/api/orders/{id}/status` | Admin |

### Addresses
| Method | Endpoint | Access |
|---|---|---|
| GET/POST | `/api/addresses` | Authenticated |
| PUT/DELETE | `/api/addresses/{id}` | Authenticated (owner only) |

### Payments
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/payments/initialize` | Authenticated |
| POST | `/api/payments/webhook` | Public (Paystack-signed) |

### User
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/users/profile` | Authenticated |

All responses follow a standard envelope:
```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

## Architecture Notes

- **Stock protection**: order creation (both manual and cart checkout) uses an atomic conditional `UPDATE ... WHERE stock_quantity >= quantity` for each item, preventing overselling under concurrent requests — verified with unit tests simulating the insufficient-stock case.
- **Order integrity**: totals are always computed server-side from the current product price, never trusted from the client. Shipping addresses are snapshotted into the order at checkout time, so later edits to a saved address never alter historical orders.
- **Cart design**: a single `CartItem` table keyed by `(user_id, product_id)` — no separate `Cart` entity needed, since each user has exactly one implicit cart. Adding an already-present product merges quantities rather than duplicating rows.
- **Ownership checks**: cart items and addresses return a `404` (not `403`) when a user tries to access someone else's resource by ID — avoids confirming that a given ID exists at all.
- **Webhook security**: Paystack webhook payloads are verified via HMAC-SHA512 signature before processing, and an idempotency table prevents duplicate event processing on retried webhooks.

## Testing

Run the test suite:
```
mvnw test
```

Covers `ProductService` (CRUD, atomic stock decrement) and `OrderService` (multi-item order creation, total calculation, transactional rollback on out-of-stock, address ownership validation).

## Frontend

This repository is backend-only. A companion React frontend consumes this API (not included here).
```


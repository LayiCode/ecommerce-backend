# Ecommerce Backend

A Spring Boot REST API backend for an e-commerce platform, built as a portfolio/learning project. Covers product catalog management, JWT-based authentication with role-based access control, Google OAuth2 social login, a persistent server-side cart, order processing with race-condition-safe stock handling, product reviews from verified buyers, a wishlist, address management, payment integration via Paystack, transactional email notifications via Brevo, and product image uploads via Supabase Storage.

## Tech Stack

- **Java 21**, **Spring Boot 4.1.0**
- **PostgreSQL 18** — database
- **Spring Data JPA / Hibernate** — ORM
- **Spring Security + JWT** — authentication and authorization
- **Spring Security OAuth2** — Google social login
- **Paystack** — payment processing (test mode)
- **Brevo** — transactional email (password reset, order confirmation, shipping, review requests)
- **Supabase Storage** — product image hosting
- **springdoc-openapi (Swagger UI)** — API documentation
- **JUnit 5 + Mockito** — unit testing
- **Docker / Docker Compose** — containerized deployment
- **Maven** — build tool
- **Lombok** — boilerplate reduction

## Features

- **Product catalog** — CRUD, search, category filtering, pagination, single image URL per product
- **Product reviews** — verified-buyer CRUD (must have a delivered order for the product), rating and review count aggregated on product responses, one review per user per product enforced at the database level
- **Wishlist** — add, remove, check, and list wishlisted products per user (idempotent)
- **Categories** — CRUD
- **Authentication** — register, login, JWT issuance, forgot/reset password (3-step: request code, verify code, reset password)
- **Google OAuth2 login** — social login with automatic account creation, merges by email
- **Role-based access control** — `CUSTOMER` and `ADMIN` roles; product/category writes, order status updates, and image uploads are admin-only
- **Cart** — persistent, server-side, tied to the logged-in user; add/update/remove items, auto-merges duplicate product entries
- **Orders** — two ways to place an order: manually specifying items, or checking out directly from the cart. Both share the same atomic, race-condition-safe stock decrement, server-computed totals, and address snapshotting at checkout. Orders can be cancelled (PENDING/PAID only) with automatic stock restoration
- **Addresses** — saved shipping addresses per user with deduplication
- **Payments** — Paystack integration: transaction initialization, webhook handling with HMAC signature verification and idempotency protection against duplicate events
- **Order lifecycle emails** — Brevo: confirmation on successful payment, shipped notification, review request with deep-links on delivery
- **Image uploads** — Supabase-backed image upload endpoint (admin-only, 5 MB limit, image/* only)
- **User profile** — update name/email, change password

## Getting Started

### Prerequisites
- Java 21
- Maven (or use the included `mvnw` wrapper)
- PostgreSQL 18 running locally
- A [Paystack](https://paystack.com) account (free, test mode) for payment features
- A [Brevo](https://www.brevo.com) account (free tier) for transactional emails
- A [Supabase](https://supabase.com) project with a `product-images` storage bucket for image uploads
- A [Google Cloud Console](https://console.cloud.google.com) project with OAuth2 credentials for Google login
- Docker and Docker Compose (optional, for containerized setup)

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
   - Your Postgres password (`spring.datasource.password`)
   - A JWT secret (`jwt.secret` — any long random string, 32+ characters)
   - Your Paystack test secret key (`paystack.secret-key`)
   - Your Brevo API key, sender email, and sender name (`brevo.*`)
   - Your Supabase URL, service-role key, and bucket name (`supabase.*`)
   - Your Google OAuth2 client ID and client ID (`spring.security.oauth2.client.registration.google.*`)
   - Your frontend URL (`app.frontend-url`)

4. **Build and run** (manual)
   ```
   mvnw clean install
   mvnw spring-boot:run
   ```

   **Or use Docker Compose** (starts both Postgres and the app):
   ```
   docker-compose up --build
   ```

5. **Explore the API**
   Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## API Overview

### Auth
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| POST | `/api/auth/forgot-password` | Public |
| POST | `/api/auth/verify-reset-code` | Public |
| POST | `/api/auth/reset-password` | Public |
| GET | `/oauth2/authorization/google` | Public (redirects to Google) |

### Products & Categories
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products` | Public |
| GET | `/api/products/{id}` | Public |
| POST | `/api/products` | Admin |
| PUT | `/api/products/{id}` | Admin |
| DELETE | `/api/products/{id}` | Admin |
| GET | `/api/categories` | Public |
| GET | `/api/categories/{id}` | Public |
| POST | `/api/categories` | Admin |

### Reviews
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/products/{id}/reviews` | Public |
| GET | `/api/reviews/mine?productId=` | Authenticated |
| POST | `/api/reviews` | Authenticated (verified buyer only) |
| PUT | `/api/reviews/{id}` | Authenticated (owner only) |
| DELETE | `/api/reviews/{id}` | Authenticated (owner or admin) |

### Wishlist
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/wishlist` | Authenticated |
| POST | `/api/wishlist/{productId}` | Authenticated |
| DELETE | `/api/wishlist/{productId}` | Authenticated |
| GET | `/api/wishlist/{productId}/check` | Authenticated |

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
| GET | `/api/orders` | Authenticated — my orders |
| GET | `/api/orders/admin/all` | Admin |
| PUT | `/api/orders/{id}/status` | Admin |
| POST | `/api/orders/{id}/cancel` | Authenticated (owner only, PENDING/PAID only) |

### Addresses
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/addresses` | Authenticated |
| POST | `/api/addresses` | Authenticated |
| PUT | `/api/addresses/{id}` | Authenticated (owner only) |
| DELETE | `/api/addresses/{id}` | Authenticated (owner only) |

### Payments
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/payments/initialize` | Authenticated |
| POST | `/api/payments/webhook` | Public (Paystack-signed) |
| GET | `/api/payments/verify/{reference}` | Authenticated |

### User
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/users/profile` | Authenticated |
| PUT | `/api/users/profile` | Authenticated |
| PUT | `/api/users/profile/password` | Authenticated |

### Uploads
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/uploads` | Admin |

All responses follow a standard envelope:
```json
{
  "success": true,
  "message": "OK",
  "data": { ... }
}
```

## Architecture Notes

- **Stock protection**: order creation (both manual and cart checkout) uses an atomic conditional `UPDATE ... WHERE stock_quantity >= quantity` for each item, preventing overselling under concurrent requests. Items are sorted by `productId` before processing to ensure consistent lock ordering and prevent deadlocks.
- **Order integrity**: totals are always computed server-side from the current product price, never trusted from the client. Shipping addresses are snapshotted into the order at checkout time as plain text fields, so later edits to a saved address never alter historical orders.
- **Cart design**: a single `CartItem` table keyed by `(user_id, product_id)` — no separate `Cart` entity needed, since each user has exactly one implicit cart. Adding an already-present product merges quantities rather than duplicating rows.
- **Ownership checks**: cart items, addresses, reviews, and orders return a `404` (not `403`) when a user tries to access someone else's resource by ID — avoids confirming that a given ID exists at all.
- **Verified-buyer reviews**: only users with at least one `DELIVERED` order containing the product can create a review. Enforced via `OrderRepository.hasDeliveredOrderForProduct`. One review per user per product enforced by a unique database constraint.
- **Order lifecycle emails**: confirmation email sent on successful Paystack payment, shipped notification on status change to SHIPPED, review request with deep-links on status change to DELIVERED. All sent via Brevo; emails are logged to stdout when Brevo is unconfigured (dev mode).
- **Order cancellation**: only allowed when status is PENDING or PAID. Automatically restores stock for every item via unconditional increment.
- **OAuth2 user merging**: Google login merges by email, preserving the existing role. New OAuth2 users are created with a random placeholder password since they authenticate via Google only.
- **Webhook security**: Paystack webhook payloads are verified via HMAC-SHA512 signature before processing, and an idempotency table prevents duplicate event processing on retried webhooks.
- **Address deduplication**: creating an address with matching `(user, fullName, line1, city, postalCode, country)` updates the existing record instead of creating a new one.

## Testing

Run the test suite:
```
mvnw test
```

48 unit tests across 7 test classes:

| Test Class | Tests | Coverage |
|---|---|---|
| `AuthServiceTest` | 9 | Password reset flow: forgot/verify/reset codes, expiry, one-time use |
| `ProductServiceTest` | 10 | CRUD, rating aggregation, atomic stock decrement success/failure |
| `OrderServiceTest` | 10 | Multi-item creation, total calculation, stock decrement rollback, status emails (SHIPPED/DELIVERED) |
| `PaymentServiceTest` | 3 | Webhook idempotency, charge.success/failed handling |
| `ReviewServiceTest` | 12 | Verified-buyer gate, duplicate prevention, ownership checks, CRUD |
| `GlobalExceptionHandlerTest` | 3 | Validation errors, data integrity violations |
| `EcommerceApplicationTests` | 1 | Context load (requires local Postgres) |

## Infrastructure

### Docker Compose
```
docker-compose up --build
```
Starts PostgreSQL 18 and the Spring Boot app. Environment variables are read from your `.env` file. Data persists in a named volume.

### GitHub Actions
A Uptime ping workflow runs every 5 minutes to keep the Render free-tier deployment awake.

## Frontend

This repository is backend-only. A companion React frontend consumes this API (separate repository, deployed on Vercel).

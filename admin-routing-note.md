# Product Reviews — API Contract (for the frontend/Vercel dev)

Feature: customers can review products they've purchased and received (verified via a DELIVERED order),
one review per user per product, with edit/delete of their own review.

## Endpoints

### Public
`GET /api/products/{id}/reviews?page=0&size=10`
Response: `{ "success": true, "data": { "content": [ReviewResponse], "totalElements": 3, ... } }`
404 if the product does not exist.

### Authenticated (send `Authorization: Bearer <jwt>`)
`GET /api/reviews/mine?productId={id}` → `{ "success": true, "data": ReviewResponse | null }`

`POST /api/reviews`
Body: `{ "productId": 1, "rating": 5, "comment": "Great!" }` (rating 1–5 required, comment ≤ 1000 chars)
→ 201 `{ "success": true, "data": ReviewResponse }`
Errors:
- 404 `Product not found: {id}`
- 403 `You can only review products you have purchased and received`
- 409 `You have already reviewed this product`

`PUT /api/reviews/{id}`
Body: same as POST (productId must match the review's product; only the owner can edit) → 200 ReviewResponse.
Errors: 404 review missing, 403 not your review.

`DELETE /api/reviews/{id}` → 200 `{ "success": true, "message": "Review deleted" }`
Errors: 404 review missing, 403 not your review.

`GET /api/auth/me` → the current user's profile (use to know if logged in + their name).

## ReviewResponse
```json
{
  "id": 10,
  "productId": 1,
  "productName": "Wireless Headphones",
  "userName": "Test User",
  "rating": 5,
  "comment": "Great!",
  "createdAt": "2026-08-11T12:00:00"
}
```

## Product payload change
Product objects (list + detail) now include two extra fields:
```json
{
  "rating": 4.5,        // average rating, null when no reviews
  "reviewCount": 3
}
```

## UI suggestions
- Product page: show average + review count near the price (e.g. "★ 4.5 (3)").
- Reviews section (`<div id="reviews">`) below the product: average, then the list.
- Logged-in user: "Write a review" star picker (1–5) + comment. If `GET /api/reviews/mine` returns a review,
  show theirs with Edit/Delete buttons (prefill form on edit).
- Guests: "Log in to review" prompt.
- Delivered-order email deep-links to `{FRONTEND_URL}/products/{id}#reviews`.
- Handle the 403/409/404 messages from POST/PUT/DELETE as inline alerts.

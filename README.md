# JSON – Jastip Online Nasional
Advanced Programming – Group Preparation

## Group Information
- Group Name: JSON – Jastip Online Nasional
- Group ID: Group-1-KKI-Adpro

### Group Members & Module PICs
- Authentication & Profile: Tara Nirmala Anwar (2406365276)
- Inventory & Catalog: Evelyne Octaviana Benedicta Aritonang (2406365282)
- Order: Sultanadika Shidqi M (2406365326)
- Wallet & Transaction: Muhammad Rifqi AlGhan (2406365396)
- Voucher & Promo: Melanton Gabriel Siregar (2406365364)

## Project Overview
JSON (Jastip Online Nasional) is a consignment and limited-goods sales platform that connects travelers (Jastipers) and buyers (Titipers).

This repository demonstrates the Group Preparation milestone, focusing on:
- System architecture planning
- Module responsibility distribution
- Backend–database integration
- CI/CD readiness

## Tech Stack
- Java 21
- Spring Boot
- H2 Database (in-memory)
- Gradle
- GitHub Actions (CI + Dummy CD)

## How to Run Locally
Run:
./gradlew bootRun

Then open:
http://localhost:8080/api/health

Expected response example:
JSON backend alive. userCount=1

This endpoint demonstrates backend and database integration using H2 and JPA.
The repository also includes a basic landing page setup for frontend integration.

## CI/CD
- Continuous Integration (CI) is implemented using GitHub Actions.
- CI runs automatically on every push and pull request.
- CI executes automated tests using: ./gradlew test
- A dummy Continuous Deployment (CD) job is implemented.
- The dummy CD job is guarded by CI and only runs if CI passes successfully.

## Deployment
Deployment URL is not provided for this submission due to AWS Education license delay, as stated in the course announcement.
Deployment preparation will be performed at the 25% milestone.

## Milestones & Work Plan

## ✅ Preparation
- Requirement analysis & system scope definition — All Members
- Tech stack decision & repository setup — Tara Nirmala Anwar
- CI/CD setup & backend + DB integration demo — Tara Nirmala Anwar
- Landing page / frontend setup — Muhammad Rifqi AlGhan

## ✅ 25% Milestone — System Foundation (Vertical Slice Ready)

**Goal:** Every module is runnable locally with its **core data model + basic endpoints**.  
Authentication is stable so everyone can build and test with real users.

### Tara — Authentication & Profile (PIC) 
**Entities**
- `User(id, email, passwordHash, username, fullName, role, status, createdAt)`
- Enums:
  - `Role { TITIPER, JASTIPER, ADMIN }`
  - `AccountStatus { ACTIVE, BANNED, PENDING_VERIFICATION }`

**Endpoints**
- `POST /api/auth/register` → create user (default role = `TITIPER`)
- `POST /api/auth/login` → returns JWT
- `GET /api/auth/me` → returns current user info from JWT
- `PATCH /api/auth/profile` → set/update `username` + `fullName`
  - If username missing → auto-generate from email local-part (example: `budi@gmail.com → budi`)
  - Username must be unique

**Security**
- Stateless security configuration using Spring Security (SessionCreationPolicy.STATELESS).
- Public endpoints:
  - POST `/api/auth/register`
  - POST `/api/auth/login`
  - GET `/api/health`
  - `/error` and static assets (`/`, `/index.html`, `/css/**`, `/js/**`, `/images/**`)
- All other endpoints require JWT authentication.
- JWT tokens are generated during login and must be provided using:
  Authorization: Bearer <token>
- Authenticated endpoints can retrieve the current user identity from the JWT via Spring Security `Authentication`.

**Integration contract for others**
- Other modules can rely on: `userId/email/role/status` from JWT and/or `/api/auth/me`.

---

### Evelyne — Inventory & Catalog (PIC)
**Entities**
- `CatalogItem(id, jastiperId, name, description, price, stock, origin, purchaseDate, createdAt, updatedAt)`

**Endpoints (basic)**
- `POST /api/catalog` (for 25% can be open for testing; role enforcement will be strengthened later)
- `GET /api/catalog` (browse all items)
- `GET /api/catalog/search?keyword=`
- `GET /api/catalog/jastiper/{jastiperId}`

**Notes**
- 25% focuses on structure + browsing/search.  
- Concurrency/war stock safety will be implemented later.

---

### Sultan — Order (PIC)
**Entities**
- `Order(id, buyerId, jastiperId, status, shippingAddress, totalPrice, createdAt, updatedAt)`
- `OrderItem(orderId, catalogItemId, qty, priceSnapshot)`

**Endpoints (basic)**
- `POST /api/orders` (create order draft with items + address)
- `GET /api/orders/me` (buyer history)
- `GET /api/orders/jastiper/me` (jastiper to-do list)

**Status scope**
- Minimal status at 25%: `PENDING`, `PAID` (full lifecycle comes at 50%)

**Notes**
- At 25% the order data model exists and can be queried.
- Deep validation (wallet, inventory, voucher) comes at 50%.

---

### Rifqi — Wallet & Transaction (PIC)
**Entities**
- `Wallet(userId, balance)`
- `WalletTransaction(id, userId, type, amount, status, timestamp, description)`
- Enums:
  - `Type { TOPUP, WITHDRAW, PAYMENT, REFUND }`
  - `Status { PENDING, SUCCESS, FAILED }`

**Endpoints (basic)**
- `GET /api/wallet/balance`
- `POST /api/wallet/topup` (simulation for now; creates tx and marks success)
- `GET /api/wallet/transactions`

**Notes**
- 25%: wallet exists, balance updates correctly, and transaction history is stored.

---

### Gabriel — Voucher & Promo (PIC)
**Entities**
- `Voucher(code, quota, startAt, endAt, terms, discountType, discountValue, active)`
- Optional:
  - `VoucherUsage(id, voucherCode, orderId, userId, usedAt)`

**Endpoints (basic)**
- `POST /api/admin/vouchers` (create; can be open for 25% testing)
- `GET /api/vouchers` (public list of active vouchers)
- `GET /api/vouchers/{code}` (voucher detail)

**Notes**
- 25%: voucher data model + listing works.
- Validation + quota reduction happens at 50%+.

---

## ✅ 50% Milestone — Business Logic + Integration

**Goal:** Real workflow works end-to-end:
**Checkout validates stock + wallet + voucher**, and lifecycle + refunds function correctly.

### Tara — Auth & Profile (PIC)
**KYC + Admin Validation**
- `POST /api/kyc/apply` → status becomes `PENDING_VERIFICATION`
  - Minimum: `fullName` (matching identity)
  - Optional: id image / social links
- `GET /api/admin/kyc/pending`
- `POST /api/admin/kyc/{userId}/approve` → user becomes `JASTIPER`
- `POST /api/admin/kyc/{userId}/reject`

**Public Profile View**
- `GET /api/users/{username}`
  - If Jastiper: show extra info + transaction statistics (initial placeholder; real stats at 75% when Order feeds it)

**User Monitoring**
- `GET /api/admin/users`
- `PATCH /api/admin/users/{userId}` → ban/demote/promote rules

**Integration**
- Provide reusable checks/services for modules:
  - user exists
  - not banned
  - role checks

---

### Evelyne — Inventory & Catalog (PIC)
**Full Catalog CRUD**
- `PATCH /api/catalog/{id}` (edit description/price/stock)
- `DELETE /api/catalog/{id}`

**Admin Monitoring**
- `GET /api/admin/catalog`
- `DELETE /api/admin/catalog/{id}`

**Integration contract for Order**
- `GET /api/catalog/{id}` returns `price + stock + owner/jastiperId`
- Stock reservation approach prepared:
  - Either endpoint (e.g., `POST /api/catalog/{id}/reserve`) or internal service contract
  - Concurrency-safe implementation comes at 75%

---

### Sultan — Order (PIC)
**Checkout Validation**
When `POST /api/orders`:
- Validate stock availability via Inventory
- Validate sufficient wallet balance via Wallet
- Apply voucher discount if code provided via Voucher
- If all valid → set initial status `PAID` (or keep `PENDING` and switch after payment logic, depending on design)

**Lifecycle**
- `PATCH /api/orders/{id}/status`
- Enforce transitions:
  - `PAID → PURCHASED → SHIPPED → COMPLETED`
- No jumping steps allowed

**Cancellation**
- `POST /api/orders/{id}/cancel` (jastiper)
- Must trigger wallet refund automatically

**Integration**
Order module calls:
- Inventory: reserve/reduce stock
- Wallet: deduct/refund
- Voucher: validate/apply + usage when payment succeeds

---

### Rifqi — Wallet & Transaction (PIC)
**Payment & Refund**
- `POST /api/wallet/deduct` (internal use by Order)
- `POST /api/wallet/refund` (internal use by cancellations)

**Withdraw Flow**
- `POST /api/wallet/withdraw` → creates transaction with `PENDING`
- Admin verification (simple flow) → `SUCCESS` or `FAILED`

**Rules**
- Balance must never be negative
- Every balance change creates a transaction record

---

### Gabriel — Voucher & Promo (PIC)
**Validation + Usage**
- `POST /api/vouchers/validate` (code, orderTotal) → returns discount amount
- `POST /api/vouchers/use` (code, orderId, userId) → decreases quota (must not go below zero)

**Admin Management**
- `PATCH /api/admin/vouchers/{code}`:
  - extend validity
  - add quota (if not expired)
  - deactivate early

---

## ✅ 75% Milestone — War/Concurrency + Rating + Reputation

**Goal:** Show “Advanced Programming” value:
concurrency-safe stock/quota handling, stress tests, and reputation system.

### Concurrency / “War” (All Members, led by Order + Inventory + Voucher)
- Inventory stock must never go negative under concurrent checkout
- Voucher quota must never go negative under concurrent usage
- High burst order creation remains consistent

**Expected implementation techniques (choose based on design)**
- Transaction isolation + locking strategy
- Optimistic locking (`@Version`)
- Pessimistic locking (`SELECT ... FOR UPDATE`)
- Reservation + commit/rollback pattern

### Sultan (Order) + Evelyne (Inventory) — lead stock safety
- Safe reserve stock workflow
- Ensure no overselling
- Add stress tests / load simulation

### Tara (Auth/Profile) + Sultan (Order) — lead rating & reputation
**Rating endpoints**
- `POST /api/orders/{id}/rating` (jastiperRating, productRating, optional review)
- Only allowed after status is `COMPLETED`

**Reputation**
- Store aggregates (avg rating, total transactions, success rate)
- Public profile of Jastiper shows minimum required stats

---

## ✅ 100% Milestone — Final Integration + Performance + Presentation

**Goal:** Deployable, stable system with full module integration and final demo flow.

### All Members
- Full integration across all modules
- End-to-end tests
- Performance improvements + cleanup
- Final documentation + presentation readiness

## Repository
This repository can be run locally by the teaching assistant and already demonstrates:
- Backend
- Frontend (landing page)
- Database integration
- CI with quality checks
- Dummy CD guarded by CI

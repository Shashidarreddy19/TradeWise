# Trade Platform — Spring Boot Backend

Export intelligence backend for Indian SMEs. Supports two user roles: **Exporter** and **Logistics Partner**.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8.x |
| Build | Maven |
| Utilities | Lombok, Bean Validation |

---

## Project Structure

```
src/main/java/com/trade/
├── TradeBackendApplication.java
├── config/
│   ├── SecurityConfig.java          # JWT security, CORS, role-based auth
│   └── DataSeeder.java              # Seeds countries, categories, compliance
├── controller/
│   ├── AuthController.java          # POST /api/auth/register|login, GET /api/auth/profile
│   ├── CountryController.java       # GET /api/countries
│   ├── CategoryController.java      # GET /api/categories
│   ├── ProductController.java       # CRUD /api/products (EXPORTER)
│   ├── OrderController.java         # CRUD /api/orders (EXPORTER)
│   ├── ShipmentController.java      # CRUD /api/shipments (LOGISTICS)
│   ├── MarketAnalysisController.java# POST /api/market-analysis (EXPORTER)
│   └── DashboardController.java     # GET /api/dashboard/exporter|logistics
├── service/
│   ├── AuthService.java             # Interface
│   ├── ProductService.java
│   ├── OrderService.java
│   ├── ShipmentService.java
│   ├── MarketAnalysisService.java
│   ├── DashboardService.java
│   └── impl/                        # All implementations
├── repository/                      # Spring Data JPA repositories
├── entity/                          # JPA entities
├── dto/                             # Request/Response DTOs only (no entity exposure)
├── security/
│   ├── JwtUtil.java                 # Token generation & validation
│   ├── JwtAuthenticationFilter.java # Intercepts every request
│   └── UserDetailsServiceImpl.java  # Loads user by email
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── BadRequestException.java
│   └── UnauthorizedException.java
└── util/
    ├── MappingUtil.java             # Entity → DTO mapping
    └── TrackingNumberUtil.java      # Tracking number generator
```

---

## Prerequisites

- Java 21
- MySQL 8.x running locally
- Maven 3.9+

---

## Setup

### 1. Create MySQL database

```sql
CREATE DATABASE trade_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=root    # change to your MySQL password
```

### 3. Run the application

```bash
mvn spring-boot:run
```

On first startup, `DataSeeder` automatically inserts:
- 5 countries (Germany, United States, UAE, Singapore, Australia)
- 5 product categories (Agricultural Products, Spices, Textiles, Food Products, Handicrafts)
- 25 compliance records (every country × category combination with realistic data)

The server starts on **http://localhost:8080**

---

## API Reference

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login, get JWT |
| GET | `/api/auth/profile` | Bearer | Get own profile |

### Reference Data

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/countries` | Public | List all 5 countries |
| GET | `/api/categories` | Public | List all 5 categories |

### Products (EXPORTER role)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List own products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

### Orders (EXPORTER role)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/orders` | List own orders |
| GET | `/api/orders/{id}` | Get order by ID |
| POST | `/api/orders` | Create order |
| PUT | `/api/orders/{id}` | Update order (PENDING only) |
| PATCH | `/api/orders/{id}/status` | Update order status |

Order status flow: `PENDING → ACCEPTED → SHIPPED → DELIVERED`

### Shipments (LOGISTICS role)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/shipments` | List own shipments |
| GET | `/api/shipments/{id}` | Get shipment by ID |
| POST | `/api/shipments` | Create shipment (order must be ACCEPTED) |
| PATCH | `/api/shipments/{id}/status` | Update shipment status |

Shipment status flow: `ASSIGNED → PICKED_UP → AT_EXPORT_CUSTOMS → IN_TRANSIT → AT_IMPORT_CUSTOMS → OUT_FOR_DELIVERY → DELIVERED`

### Market Analysis (EXPORTER role)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/market-analysis` | Get compliance data for product + country |

Request body:
```json
{ "countryId": 1, "productId": 1 }
```

Returns customs duty, required certificates, required documents, restricted items, transit time, and recommended port — all from the Compliance table. No AI used.

### Dashboard

| Method | Endpoint | Role | Description |
|---|---|---|---|
| GET | `/api/dashboard/exporter` | EXPORTER | Products, order counts, pending shipments |
| GET | `/api/dashboard/logistics` | LOGISTICS | Assigned, completed, pending deliveries |

---

## Authentication Flow

```
POST /api/auth/register  →  { "data": { "token": "eyJ..." } }
                                        ↓
                             Add to all requests:
                             Authorization: Bearer eyJ...
```

---

## Postman Testing

1. Import `Trade-API.postman_collection.json` into Postman
2. Run **Register Exporter** — token is auto-saved to `{{token}}`
3. Test products, orders, market analysis with the exporter token
4. Run **Register Logistics Partner** — token switches to logistics
5. Test shipment creation and status updates
6. Switch tokens using **Login as Exporter** / **Login as Logistics Partner**

---

## RAG-Ready Architecture

This backend is intentionally designed for future AI/RAG integration:

- `MarketAnalysisService` returns structured compliance data — a RAG module at `/api/rag/*` can call this internally and enrich the response with LLM insights
- The `Compliance` entity text fields (`requiredCertificates`, `requiredDocuments`, etc.) map directly to retrievable text chunks for vector embedding
- No schema changes are required to add a RAG layer

---

## Seeded Reference IDs (for Postman)

### Countries
| ID | Name | Currency |
|---|---|---|
| 1 | Germany | EUR |
| 2 | United States | USD |
| 3 | United Arab Emirates | AED |
| 4 | Singapore | SGD |
| 5 | Australia | AUD |

### Categories
| ID | Name |
|---|---|
| 1 | Agricultural Products |
| 2 | Spices |
| 3 | Textiles |
| 4 | Food Products |
| 5 | Handicrafts |

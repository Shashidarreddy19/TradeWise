# CBEC-AI Trade Intelligence Platform — Full Technical Documentation

**Version:** Production  
**Date:** August 2026  
**Stack:** React 18 + Vite · Spring Boot 3.3 · MySQL 8 (dual DB) · NVIDIA LLM API

---

## 1. SYSTEM ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────────┐
│                     FRONTEND (React 18 + Vite)                      │
│                        localhost:5173                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────────────────┐  │
│  │  Login / │  │ Exporter │  │ Logistics Dashboard              │  │
│  │ Register │  │Dashboard │  │ (orders, shipments, tracking)    │  │
│  └──────────┘  └──────────┘  └──────────────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────────┘
                            │ HTTP (JWT Bearer)
┌───────────────────────────▼─────────────────────────────────────────┐
│              SPRING BOOT BACKEND  localhost:8081                     │
│  ┌──────────────────────┐  ┌──────────────────────────────────────┐ │
│  │  Core API (auth,     │  │  Intelligence API (/api/v1/*)        │ │
│  │  products, orders,   │  │  - Regulatory retrieval (RAG)        │ │
│  │  shipments, market)  │  │  - AI chat (NVIDIA LLM)              │ │
│  └──────────┬───────────┘  │  - Cost estimation                   │ │
│             │              │  - Market opportunity ranking         │ │
│  ┌──────────▼───────────┐  │  - Government incentives             │ │
│  │  InternationalTrade  │  │  - Negotiation assistant             │ │
│  │  (MySQL primary DB)  │  └──────────────┬───────────────────────┘ │
│  └──────────────────────┘                 │                         │
└──────────────────────────────────────────┼─────────────────────────┘
                                           │
             ┌─────────────────────────────┼──────────────────────┐
             │          ▼                  │           ▼           │
    ┌────────────────────┐      ┌──────────────────────────────┐   │
    │  TradeData (MySQL  │      │  NVIDIA AI API               │   │
    │  secondary DB)     │      │  integrate.api.nvidia.com/v1 │   │
    │  63,746 HS codes   │      │  Model: llama-3.1-nemotron   │   │
    │  36 regulations    │      │  ultra-253b-v1               │   │
    │  106 sources       │      │  (requires NVIDIA_API_KEY)   │   │
    │  7 incentives      │      └──────────────────────────────┘   │
    │  11 countries      │                                          │
    └────────────────────┘                                          │
```

---

## 2. DUAL DATABASE ARCHITECTURE

### Primary DB — `InternationalTrade` (Spring Boot managed)
Handles all transactional data for the core trade workflow.

| Table | Purpose |
|---|---|
| `users` | Authentication, roles (EXPORTER / LOGISTICS) |
| `exporter_profiles` | Company, GST, IEC, business type |
| `logistics_profiles` | Company, fleet, service area |
| `product_categories` | 24 canonical product categories |
| `products` | Exporter's product catalog |
| `countries` | 5 destination countries for orders |
| `orders` | Shipment requests from exporters |
| `order_rejections` | Rejection reasons from logistics |
| `shipments` | Active shipments managed by logistics |

**Config class:** `com.trade.config.PrimaryDatasourceConfig`  
**Transaction manager:** `transactionManager` (default)  
**DDL strategy:** `update` (Hibernate managed)

### Secondary DB — `TradeData` (Read-only from backend)
Holds regulatory and trade intelligence. Populated by the data pipeline.

| Table | Rows | Purpose |
|---|---|---|
| `hs_master` | 63,746 | Official HS code descriptions by country |
| `regulation_master` | 36 | Published regulations per country |
| `regulation_hs_mapping` | 76 | HS code → regulation linkage |
| `regulation_documents` | 72 | Required export documents |
| `regulation_certifications` | 72 | Required certifications |
| `regulation_labeling` | ~0 | Labeling requirements |
| `regulation_restrictions` | ~0 | Prohibitions and restrictions |
| `regulation_procedures` | ~0 | Customs procedures |
| `regulation_source` | 106 | Official government source URLs |
| `country_master` | 11 | Supported destination countries |
| `hs_regulatory_coverage_audit` | 11,500 | HS coverage verification audit |
| `regulation_hs_evidence_verification` | 10,919 | Evidence-based HS mapping |
| `government_incentives` | 7 | Real government export schemes |
| `hs_validated` | 9,795 | Pipeline-validated HS codes |

**Config class:** `com.trade.config.TradeDataDatasourceConfig`  
**Transaction manager:** `tradeDataTransactionManager`  
**Entity scan:** `com.trade.regulatory.entity`  
**Repository scan:** `com.trade.regulatory.repository`  
**DDL strategy:** `none` (read-only — never modifies schema)

---

## 3. AUTHENTICATION & AUTHORIZATION

### Flow
1. User registers (`POST /api/auth/register`) → credentials stored in `InternationalTrade.users`
2. User logs in (`POST /api/auth/login`) → JWT issued (24h expiry)
3. All subsequent requests carry `Authorization: Bearer <token>`
4. `JwtAuthenticationFilter` validates token on every request
5. `UserDetailsServiceImpl` loads user from DB and sets Spring Security authorities

### Role Normalization
The DB stores legacy role values. `UserDetailsServiceImpl` maps them:

| DB Value | Spring Security Authority | Frontend Role |
|---|---|---|
| `EXPORTER` | `ROLE_EXPORTER` | `EXPORTER` |
| `LOGISTICS` | `ROLE_LOGISTICS` | `LOGISTICS` |
| `LOGISTICS_PARTNER` | `ROLE_LOGISTICS` (normalized) | `LOGISTICS` |
| `IMPORTER` | `ROLE_EXPORTER` (normalized) | `EXPORTER` |
| `ADMIN` | `ROLE_EXPORTER` (normalized) | `EXPORTER` |

### URL Security Rules
| Pattern | Access |
|---|---|
| `/api/auth/**` | Public |
| `/api/countries`, `/api/categories` | Public |
| `/api/v1/**` | Authenticated (any role) |
| `/api/products/**` | `ROLE_EXPORTER` only |
| `/api/orders/**` | Authenticated + method-level `@PreAuthorize` |
| `/api/shipments/**` | `ROLE_LOGISTICS` only |
| `/api/dashboard/exporter` | `ROLE_EXPORTER` only |
| `/api/dashboard/logistics` | `ROLE_LOGISTICS` only |

### Account Lockout
- 5 failed attempts → 15-minute lockout stored in `users.lockout_until`
- Failure counter resets on successful login

### Demo Accounts (password: `password123`)
| Email | Role |
|---|---|
| `exporter@company.com` | EXPORTER |
| `logistics@company.com` | LOGISTICS |
| `testuser999@test.com` | EXPORTER |

---

## 4. RAG IMPLEMENTATION — HOW IT WORKS

RAG (Retrieval Augmented Generation) in this project means:
**retrieve real regulatory data from TradeData first → pass it as context to NVIDIA LLM → get grounded answer**.

The LLM is NEVER the source of truth. It only explains or summarizes data that was already retrieved from the database.

### RAG Pipeline (Step by Step)

```
User Question
     │
     ▼
Step 1: RETRIEVE — RegulatoryRetrievalService.getRegulations(country, hsCode)
     │  ┌─────────────────────────────────────────────────────────────────┐
     │  │ Hierarchical HS Matching (TradeData):                           │
     │  │   1. Try EXACT national code match in regulation_hs_mapping     │
     │  │   2. Try HS6 match (first 6 digits)                             │
     │  │   3. Try HS4 heading match (first 4 digits)                     │
     │  │   4. Try HS2 chapter match (first 2 digits)                     │
     │  │   5. Check hs_regulatory_coverage_audit for coverage data       │
     │  │   6. If nothing found → matchType=NOT_FOUND, confidence=0       │
     │  └─────────────────────────────────────────────────────────────────┘
     │
     ▼
Step 2: BUILD CONTEXT — Structured text from retrieved DB records
     │  - Regulation names and authorities
     │  - Required documents (mandatory/optional)
     │  - Required certifications
     │  - Labeling requirements
     │  - Restrictions and prohibitions
     │  - Step-by-step customs procedures
     │  - Official source URLs
     │
     ▼
Step 3: AUGMENT — Pass context to NvidiaAiService.chat()
     │  System prompt: "Answer ONLY from the provided context.
     │                  Never invent certificates, URLs, or authorities."
     │  User message: the actual question
     │  Context: the structured DB text from Step 2
     │
     ▼
Step 4: GENERATE — NVIDIA LLM (llama-3.1-nemotron-ultra-253b-v1)
     │  - Temperature: 0.2 (low, for factual responses)
     │  - Max tokens: 2048
     │  - Constrained to provided context only
     │
     ▼
Step 5: RETURN — Response with answer + source citations + confidence
```

### Confidence Levels
| Match Type | Confidence | Meaning |
|---|---|---|
| `EXACT_NATIONAL_CODE` | 1.0 | Exact HS code match found |
| `HS6` | 0.85 | 6-digit heading match |
| `HS4_HEADING` | 0.7 | 4-digit heading match |
| `HS2_CHAPTER` | 0.5 | Chapter-level match only |
| `COVERAGE_AUDIT` | variable | From pipeline audit data |
| `NOT_FOUND` | 0.0 | No regulatory data available |

---

## 5. LLM INTEGRATION — NVIDIA AI

### Configuration
```properties
nvidia.api.key=${NVIDIA_API_KEY:}            # Empty default = disabled
nvidia.base.url=https://integrate.api.nvidia.com/v1
nvidia.model=nvidia/llama-3.1-nemotron-ultra-253b-v1
```

### Current Status
**`available = false`** — `NVIDIA_API_KEY` environment variable is not set.  
To enable: set `NVIDIA_API_KEY=nvapi-xxxx` in the system environment or as a JVM argument:
```
-DNVIDIA_API_KEY=nvapi-xxxx
```

### What Works Without the Key
All structured data APIs work without the key:
- Regulatory retrieval (regulations, documents, certifications)
- Compliance scoring
- Cost estimation
- Market opportunity ranking
- Government incentives lookup
- Export guide generation

### What Requires the Key
- `POST /api/v1/ai/regulatory-chat` → grounded regulatory Q&A
- `POST /api/v1/ai/summarize-document` → document summarization
- `POST /api/v1/ai/negotiation-assistant` → negotiation advice

### NvidiaAiService Design
- **Single centralized service** — all LLM calls go through `NvidiaAiService`
- **OpenAI-compatible endpoint** — NVIDIA uses the same `/chat/completions` format
- **Graceful degradation** — if key not set or call fails, returns human-readable error message
- **No fabrication** — system prompt explicitly forbids inventing regulatory data
- **Low temperature (0.2)** — ensures consistent, factual responses

### API Call Structure
```json
POST https://integrate.api.nvidia.com/v1/chat/completions
{
  "model": "nvidia/llama-3.1-nemotron-ultra-253b-v1",
  "messages": [
    { "role": "system", "content": "<system prompt + regulatory context>" },
    { "role": "user",   "content": "<user question>" }
  ],
  "temperature": 0.2,
  "max_tokens": 2048
}
```

---

## 6. FEATURE-BY-FEATURE BREAKDOWN

### Feature 1 — Country Recommendations (Market Analysis)
**Frontend:** `handleFetchCountryRankings()` in `Exporter.jsx`  
**API:** `GET /api/v1/compliance/{country}/{hsCode}` (called per country in a loop)  
**Backend:** `RegulatoryController` → `RegulatoryRetrievalService.calculateCompliance()`  
**Data source:** `TradeData.regulation_hs_mapping` + `TradeData.regulation_*` tables

**How it works:**
1. Frontend loops through 10 candidate countries
2. For each country calls compliance endpoint
3. Gets `complianceScore` (0-100), `complexity` (LOW/MEDIUM/HIGH), document/cert counts
4. Sorts by compliance score descending
5. Displays ranked list with reason text

**Scoring Formula (in `calculateCompliance()`):**
```
Penalty = (docs × 2) + (certs × 4) + (restrictions × 6) + (labeling × 1.5) + (procedures × 1)
If confidence < 0.5: add 10 penalty
numericScore = max(10, min(100, 100 - penalty))
```

---

### Feature 2 — Export Regulations
**Frontend:** `handleFetchRegulations()` in `Exporter.jsx`  
**API:** `GET /api/v1/regulations/{country}/{hsCode}`  
**Backend:** `RegulatoryController` → `RegulatoryRetrievalService.getRegulations()`  
**Data source:** All `regulation_*` tables in `TradeData`

**Returns:** Full regulatory profile — regulations, documents, certifications, labeling, restrictions, procedures, official source URLs, match type, confidence score.

---

### Feature 3 — Explain Recommendation
**Frontend:** `handleExplainCountry()` in `Exporter.jsx`  
**API:** `GET /api/v1/compliance/{country}/{hsCode}`  
**Backend:** `RegulatoryController`  
**Data source:** `TradeData` regulatory tables  
**Notes:** Shows compliance score breakdown — document count, cert count, restriction count, complexity rating.

---

### Feature 4 — Step-by-Step Export Guidance
**Frontend:** `handleFetchGuidance()` in `Exporter.jsx`  
**API:** `GET /api/v1/requirements/{country}/{hsCode}`  
**Backend:** `RegulatoryController`  
**Data source:** `TradeData.regulation_documents`, `regulation_certifications`, `regulation_labeling`, `regulation_procedures`

---

### Feature 5 — Export Guide (Structured Checklist)
**Frontend:** `aiApi.getExportGuide(country, hsCode)`  
**API:** `GET /api/v1/export-guide/{country}/{hsCode}`  
**Backend:** `ExportGuideController`  
**Data source:** TradeData — all regulation sub-tables  
**Returns:** Numbered steps: HS classification → documents → certifications → labeling → restrictions → customs procedures → declaration submission.  Each step is derived purely from DB records.

---

### Feature 6 — AI Regulatory Chat
**Frontend:** `aiApi.regulatoryChat(country, hsCode, question)`  
**API:** `POST /api/v1/ai/regulatory-chat`  
**Backend:** `AiRegulatoryController`  
**Data flow:** TradeData retrieval → context string → NVIDIA LLM → grounded answer  
**Requires:** `NVIDIA_API_KEY` env variable  
**Returns:** `answer`, `matchType`, `confidence`, `regulationFound`, `sources[]`, `aiAvailable`

---

### Feature 7 — Document Summarizer
**Frontend:** `handleSummariseDoc()` in `Exporter.jsx`  
**API:** `POST /api/v1/ai/summarize-document`  
**Backend:** `AiRegulatoryController` → `NvidiaAiService.summarizeDocument()`  
**Data flow:** User pastes document text → sent to NVIDIA LLM → extracts requirements, documents, deadlines, restrictions  
**Requires:** `NVIDIA_API_KEY`  
**Input limit:** 8,000 characters of document text

---

### Feature 8 — Government Incentives
**Frontend:** `handleFetchIncentives()` in `Exporter.jsx`  
**API:** `GET /api/v1/incentives/India/{hsCode}` or `GET /api/v1/incentives/India`  
**Backend:** `IntelligenceController` → `GovernmentIncentivesService`  
**Data source:** `TradeData.government_incentives` (7 rows)

**Matching logic:**
1. If HS code provided: query `findApplicable(country, chapter, hs6)` — matches where `chapter_applicable` starts with HS chapter OR `hs_code_applicable` starts with HS6
2. If no specific matches found: fall back to all active schemes for that country
3. Returns only `status='ACTIVE'` schemes

**Seeded Real Schemes (India):**
| Scheme | Authority | Benefit |
|---|---|---|
| RoDTEP | DGFT | % credit on FOB value |
| EPCG | DGFT | Zero duty on capital goods import |
| Advance Authorisation | DGFT | Duty-free input import |
| PLI (Electronics) | Ministry of Finance | 4-6% on incremental sales |

**Seeded Real Schemes (Other Countries):**
| Scheme | Country | Benefit |
|---|---|---|
| GSP | United States | Zero duty for Indian exporters |
| Khalifa Fund | UAE | SME import/distribution finance |

---

### Feature 9 — Market Opportunity Ranking
**Frontend:** `handleFetchMarketOpportunity()` in `Exporter.jsx`  
**API:** `POST /api/v1/market-opportunity/rank`  
**Backend:** `IntelligenceController` → `MarketOpportunityService.rankCountries()`  
**Data source:** TradeData regulatory data + hardcoded official tariff rates

**Scoring formula (per country, transparent and deterministic):**
```
compScore    = compliance score from RegulatoryRetrievalService (0-100)
tariffPenalty = dutyRate × 2
taxPenalty    = taxRate × 0.5

opportunityScore = max(0, min(100, compScore - tariffPenalty - taxPenalty + 30))
```
The `+30` is a base market accessibility bonus (all countries start at an accessible baseline).

**Example — Cotton T-Shirts (HS 61091000):**
| Country | Duty | Comp Score | Opp Score | Rank |
|---|---|---|---|---|
| Singapore | 0% | 100 | 100 | #1 |
| United States | 16% | 100 | 88 | #2 |
| Germany | 12% | 100 | 87 | #3 |

---

### Feature 10 — Cost Estimation
**Frontend:** `handleCalculateCost()` in `Exporter.jsx`  
**API:** `POST /api/v1/cost-estimation`  
**Backend:** `IntelligenceController` → `CostEstimationService.estimateCost()`  
**Data source:** Hardcoded official MFN tariff rates + official VAT/GST rates

**Calculation:**
```
totalValue       = productValue × quantity
dutyAmount       = totalValue × (dutyRate / 100)
taxableValue     = totalValue + dutyAmount
taxAmount        = taxableValue × (taxRate / 100)
landedCost       = totalValue + dutyAmount + taxAmount
```

**Covered Countries & Key Rates:**

| Country | Default MFN | Textiles (Ch.61/62) | Electronics (Ch.85) | VAT/GST |
|---|---|---|---|---|
| United States | 3.5% | 16% | 2.5% | 0% |
| Germany (EU) | 4.2% | 12% | 2.5% | 19% |
| United Arab Emirates | 5% | 5% | 5% | 5% |
| Singapore | 0% | 0% | 0% | 9% GST |
| Australia | 5% | 5% | 0% | 10% GST |
| Japan | 3.5% | 3.5% | 0% | 10% |
| United Kingdom | 4% | 4% | 2.5% | 20% |
| Canada | 3.5% | 3.5% | 3.5% | 5% GST |
| South Korea | 8% | 8% | 8% | 10% |
| Hong Kong | 0% | 0% | 0% | 0% |
| India | 10% | 20% | 15% | 18% GST |

**When official rate unavailable:** Returns `dutySource: "COUNTRY_NOT_SUPPORTED"` or `"DATA_UNAVAILABLE"` — never fabricates.

---

### Feature 11 — Negotiation Assistant
**Frontend:** `handleNegotiation()` in `Exporter.jsx`  
**API:** `POST /api/v1/ai/negotiation-assistant`  
**Backend:** `IntelligenceController` → `NegotiationAssistantService`  
**Data flow:** TradeData regulatory retrieval → NVIDIA LLM → structured negotiation advice  
**Requires:** `NVIDIA_API_KEY`

**Input fields:**
- `product` — product name
- `hsCode` — HS code (used for regulatory grounding)
- `country` — buyer's country
- `buyerMessage` — what the buyer said
- `objective` — exporter's goal (`initial_email`, `counter_offer`, etc.)
- `quantity` — deal size
- `targetPrice` — exporter's target price

**LLM Output structure:**
1. Analysis of buyer's position
2. Suggested response text
3. Key negotiation points
4. Compliance considerations (from retrieved DB data)
5. Risks to be aware of
6. Questions to ask the buyer
7. Recommended fallback position

---

### Feature 12 — Compliance Checklist
**Frontend:** `handleFetchComplianceCheck()` in `Exporter.jsx`  
**API:** `GET /api/v1/requirements/{country}/{hsCode}`  
**Backend:** `RegulatoryController`  
**Data source:** TradeData — all regulation sub-tables

---

### Feature 13 — Country Risk Analysis
**Frontend:** `handleFetchRisk()` in `Exporter.jsx`  
**API:** `GET /api/v1/compliance/{country}/{hsCode}`  
**Backend:** `RegulatoryController`  
**Notes:** Reuses compliance endpoint. Compliance score serves as proxy for market risk — higher restriction count = higher risk.

---

### Feature 14 — Logistics Dashboard
**Frontend:** `Logistics.jsx`  
**APIs:** `GET /api/orders/pending`, `PATCH /api/orders/{id}/accept`, `PATCH /api/orders/{id}/reject`, `GET /api/shipments`, `PATCH /api/shipments/{id}/status`  
**Data source:** `InternationalTrade` primary DB  
**Features:** View pending shipment requests, accept/reject, update shipment status (IN_TRANSIT, DELIVERED, etc.)

---

### Feature 15 — Exporter Product Management
**Frontend:** Products tab in `Exporter.jsx`  
**APIs:** `GET /api/products`, `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`  
**Data source:** `InternationalTrade.products`  
**Fields:** name, description, HS code, price, weight, category

---

### Feature 16 — Order Management
**Frontend:** Orders tab in `Exporter.jsx`  
**APIs:** `GET /api/orders`, `POST /api/orders`, `PUT /api/orders/{id}`  
**Data source:** `InternationalTrade.orders`  
**Workflow:** Exporter creates order → PENDING_LOGISTICS → Logistics accepts/rejects → ACCEPTED → SHIPMENT_CREATED

---

### Feature 17 — Market Analysis (Legacy)
**Frontend:** Market Analysis section in `Exporter.jsx`  
**API:** `POST /api/market-analysis`  
**Backend:** `MarketAnalysisController` → `MarketAnalysisServiceImpl`  
**Data source:** `InternationalTrade.compliance` (lookup by country + category)  
**Returns:** Customs duty, required certificates/documents, transit time, recommended port

---

## 7. COMPLETE API REFERENCE

### Auth APIs (Public)
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/register` | Register new user (EXPORTER or LOGISTICS) |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/auth/profile` | Get authenticated user's profile |
| GET | `/api/auth/check-email?email=` | Check email availability |
| GET | `/api/auth/check-phone?phone=` | Check phone availability |

### Core APIs (Authenticated)
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/api/products` | EXPORTER | List own products |
| POST | `/api/products` | EXPORTER | Create product |
| PUT | `/api/products/{id}` | EXPORTER | Update product |
| DELETE | `/api/products/{id}` | EXPORTER | Delete product |
| GET | `/api/orders` | EXPORTER | List own orders |
| POST | `/api/orders` | EXPORTER | Create shipment request |
| GET | `/api/orders/pending` | LOGISTICS | Pending requests |
| PATCH | `/api/orders/{id}/accept` | LOGISTICS | Accept request |
| PATCH | `/api/orders/{id}/reject` | LOGISTICS | Reject request |
| GET | `/api/shipments` | LOGISTICS | List shipments |
| PATCH | `/api/shipments/{id}/status` | LOGISTICS | Update status |
| GET | `/api/dashboard/exporter` | EXPORTER | Exporter KPIs |
| GET | `/api/dashboard/logistics` | LOGISTICS | Logistics KPIs |
| POST | `/api/market-analysis` | EXPORTER | Legacy market analysis |

### Intelligence APIs — Regulatory (Authenticated, any role)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/regulations/{country}/{hsCode}` | Full regulatory profile |
| GET | `/api/v1/compliance/{country}/{hsCode}` | Compliance score (0-100) |
| GET | `/api/v1/documents/{country}/{hsCode}` | Required documents |
| GET | `/api/v1/certificates/{country}/{hsCode}` | Required certifications |
| GET | `/api/v1/requirements/{country}/{hsCode}` | All requirements aggregate |
| GET | `/api/v1/export-guide/{country}/{hsCode}` | Step-by-step export checklist |
| GET | `/api/v1/hs/search?query=&country=` | HS code search (63k codes) |
| GET | `/api/v1/hs/{hsCode}` | HS code lookup |
| GET | `/api/v1/countries` | Supported regulatory countries |
| GET | `/api/v1/dashboard/statistics` | DB coverage stats |

### Intelligence APIs — AI (Authenticated, requires NVIDIA_API_KEY)
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/ai/regulatory-chat` | RAG-grounded regulatory Q&A |
| POST | `/api/v1/ai/summarize-document` | Document summarization |
| GET | `/api/v1/ai/status` | AI service availability check |

### Intelligence APIs — Cost & Market (Authenticated)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/cost-estimation?country=&hsCode=&value=&quantity=` | Cost estimation (GET) |
| POST | `/api/v1/cost-estimation` | Cost estimation (POST with body) |
| GET | `/api/v1/market-opportunity/{hsCode}` | Rank all countries for HS code |
| POST | `/api/v1/market-opportunity/rank` | Rank specific countries |
| GET | `/api/v1/market-opportunity/{country}/{hsCode}` | Single country analysis |
| GET | `/api/v1/incentives/{country}` | Government incentives for country |
| GET | `/api/v1/incentives/{country}/{hsCode}` | Incentives for country + HS code |
| POST | `/api/v1/ai/negotiation-assistant` | AI negotiation advice (needs key) |

---

## 8. PRODUCT CATEGORIES

All 24 categories are synchronized across: `DataSeeder.java` → `InternationalTrade.product_categories` table → `AuthServiceImpl.VALID_CATEGORIES` → `Register.jsx PRODUCT_CATEGORIES` constant.

```
Agricultural Products    Spices                Food Products
Processed Foods          Marine Products       Textiles
Apparel & Garments       Home Textiles         Leather Products
Footwear                 Handicrafts           Ceramics & Pottery
Glassware                Jewellery & Gems      Chemicals
Cosmetics & Personal Care  Pharmaceuticals     Plastics & Rubber
Electronics              Engineering Goods     Machinery
Automotive Components    Furniture & Wood      Others
```

---

## 9. DATA PIPELINE STATUS

The `TradeData` database is populated by a separate pipeline project. Current state:

| Component | Status | Details |
|---|---|---|
| HS Master | ✅ POPULATED | 63,746 codes across 11 countries |
| Regulation Master | ✅ POPULATED | 36 regulations |
| Regulation Sources | ✅ POPULATED | 106 official sources |
| Regulation HS Mapping | ✅ POPULATED | 76 mappings |
| Regulation Documents | ✅ POPULATED | 72 documents |
| Regulation Certifications | ✅ POPULATED | 72 certifications |
| Regulation Labeling | ⚠️ EMPTY | Pipeline not run for this table |
| Regulation Restrictions | ⚠️ EMPTY | Pipeline not run for this table |
| Regulation Procedures | ⚠️ EMPTY | Pipeline not run for this table |
| HS Coverage Audit | ✅ POPULATED | 11,500 audit records |
| HS Evidence Verification | ✅ POPULATED | 10,919 records |
| Government Incentives | ✅ SEEDED | 7 real schemes (manual seed) |
| Tariff Statistics | ⚠️ EMPTY | Cost estimation uses hardcoded rates |
| Country Master | ✅ POPULATED | 11 countries |

**Impact of empty tables:** When labeling, restrictions, or procedures are empty, the compliance score defaults to `100 - penalty(docs, certs only)`. The RAG context omits those sections. The export guide skips those steps. This is correct behavior — not a bug.

---

## 10. KNOWN LIMITATIONS

| Area | Limitation | Impact |
|---|---|---|
| NVIDIA AI | Requires `NVIDIA_API_KEY` env var | Chat, negotiation, summarizer return error message |
| Cost Estimation | Only 11 countries with hardcoded rates | Unsupported countries return `COUNTRY_NOT_SUPPORTED` |
| Regulation Data | Labeling, restrictions, procedures tables empty | Compliance score based on docs + certs only |
| Market Opportunity | No real market demand data (tables empty) | Score based on regulatory complexity + tariff only |
| Tariff Statistics | `tariff_statistics` table is empty | Cost service uses hardcoded MFN rates, not DB |
| Regulation HS Mapping | Only 76 mappings | Many HS codes return `NOT_FOUND` |

---

## 11. ENVIRONMENT VARIABLES

| Variable | Default | Required |
|---|---|---|
| `NVIDIA_API_KEY` | *(empty)* | For AI features |
| `NVIDIA_BASE_URL` | `https://integrate.api.nvidia.com/v1` | Optional |
| `NVIDIA_MODEL` | `nvidia/llama-3.1-nemotron-ultra-253b-v1` | Optional |
| `DB_MAIN_USERNAME` | `root` | Optional |
| `DB_MAIN_PASSWORD` | `root` | Optional |
| `DB_TRADEDATA_USERNAME` | `root` | Optional |
| `DB_TRADEDATA_PASSWORD` | `root` | Optional |
| `JWT_SECRET` | *(hardcoded fallback)* | Production: set this |

---

## 12. RUNNING THE PROJECT

```bash
# Backend
cd "D:\MAJOR PROJECT\backend"
.\mvnw.cmd spring-boot:run
# Starts on http://localhost:8081

# Frontend
cd "D:\MAJOR PROJECT\frontend"
npx vite --port 5173
# Opens http://localhost:5173

# With NVIDIA AI enabled
$env:NVIDIA_API_KEY="nvapi-xxxx"
.\mvnw.cmd spring-boot:run
```

---

## 13. FILE STRUCTURE

```
D:\MAJOR PROJECT\
├── backend\src\main\java\com\trade\
│   ├── config\
│   │   ├── DataSeeder.java              — Seeds countries + 24 categories
│   │   ├── SecurityConfig.java          — JWT, CORS, role-based URL rules
│   │   ├── PrimaryDatasourceConfig.java — InternationalTrade DB config
│   │   └── TradeDataDatasourceConfig.java — TradeData DB (read-only)
│   ├── controller\
│   │   ├── AuthController.java
│   │   ├── ProductController.java
│   │   ├── OrderController.java
│   │   ├── ShipmentController.java
│   │   ├── DashboardController.java
│   │   └── MarketAnalysisController.java
│   ├── entity\   (InternationalTrade entities)
│   │   ├── User.java, Role.java
│   │   ├── Product.java, ProductCategory.java
│   │   ├── Order.java, OrderStatus.java
│   │   └── Shipment.java, ShipmentStatus.java
│   ├── security\
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── UserDetailsServiceImpl.java  — Role normalization
│   └── service\impl\
│       ├── AuthServiceImpl.java         — Registration + login
│       └── MarketAnalysisServiceImpl.java
│
├── backend\src\main\java\com\trade\regulatory\
│   ├── controller\
│   │   ├── RegulatoryController.java    — 9 regulatory endpoints
│   │   ├── AiRegulatoryController.java  — AI chat + summarize
│   │   ├── ExportGuideController.java   — Step-by-step guide
│   │   └── IntelligenceController.java  — Cost, market, incentives, negotiation
│   ├── service\
│   │   ├── RegulatoryRetrievalService.java — Hierarchical RAG retrieval
│   │   ├── NvidiaAiService.java          — NVIDIA LLM integration
│   │   ├── CostEstimationService.java    — Tariff + VAT calculation
│   │   ├── MarketOpportunityService.java — Country ranking algorithm
│   │   ├── NegotiationAssistantService.java — AI-powered negotiation
│   │   └── GovernmentIncentivesService.java — Incentive scheme lookup
│   ├── entity\   (TradeData entities — 12 entities)
│   └── repository\  (TradeData JPA repos — 12 repos)
│
└── frontend\src\
    ├── App.jsx                          — Router + auth guards
    ├── pages\
    │   ├── Login.jsx
    │   ├── Register.jsx                 — 24 categories, role-based fields
    │   ├── Exporter.jsx                 — All 17 features
    │   └── Logistics.jsx                — Shipment management
    ├── services\
    │   └── api.js                       — All API methods (authApi, regulatoryApi,
    │                                      aiApi, intelligenceApi, etc.)
    └── components\
        ├── Navbar.jsx, Hero.jsx, Footer.jsx
        └── CoreFeatures.jsx             — Landing page features showcase
```

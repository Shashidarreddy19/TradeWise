# TradeBridge - AI-Driven Cross-Border Export Intelligence Platform

An integrated platform for Indian SMEs that combines HS code classification, regulatory compliance intelligence, and ML-based export destination ranking into a single decision-support system.

## Architecture

```
Frontend (React 19 + Vite)
    │
    ▼  REST + JWT
Backend (Spring Boot 3.3 / Java 21)
    ├── Core API (Auth, Products, Orders, Shipments, Dashboard)
    ├── HS Classification Engine (DB-first + LLM re-ranking)
    ├── Regulatory Intelligence (Hybrid RAG: structured + semantic + rerank)
    └── ML Ranking Lookup (static serving from offline-trained model)
    │
    ▼  JPA
Databases
    ├── InternationalTrade DB (transactional: users, products, orders)
    └── TradeData DB (read-only: HS codes, regulations, documents)

Offline ML Pipeline (Python)
    └── CEPII BACI + Gravity + WDI → feature engineering → XGBRanker
        → rolling-origin evaluation → serving table (CSV)

Pipeline Service (Spring Boot)
    └── Parses official HS/regulatory documents → populates TradeData DB
```

## Modules

### 1. HS Code Classification
- Database-grounded, LLM-assisted classification (NVIDIA Nemotron)
- Deterministic multi-signal scoring + AI re-ranking (50/50 blend)
- Validated against India HS master; returns `NEEDS_REVIEW` for out-of-coverage products
- Confidence levels: HIGH_CONFIDENCE / GOOD_MATCH / POSSIBLE_MATCH / LOW_CONFIDENCE / NEEDS_REVIEW

### 2. Regulatory Intelligence & Compliance
- Hierarchical HS matching (exact national code → HS6 → HS4 → HS2)
- Hybrid multimodal RAG: structured DB evidence + semantic vector search + neural reranking
- Grounded LLM responses constrained to retrieved evidence only
- Returns `DATA_UNAVAILABLE` when evidence is insufficient

### 3. Export Destination Ranking (ML)
- 55 HS6 products × 10 destinations × 8 years (2017–2024)
- 30-feature set: trade history, gravity, macro, entry cost, engineered
- Leakage-audited (removed temporal proxies)
- Rolling-origin evaluation (4 origins, per-origin hyperparameter re-tuning)
- XGBRanker (pairwise) achieves highest pooled Spearman ρ = 0.8630
- Served as a static lookup table — no live model inference dependency

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 19, Vite 8, Tailwind CSS, Axios |
| Backend | Spring Boot 3.3, Java 21, Spring Security + JWT |
| Database | MySQL 8 (dual datasource via Hibernate) |
| AI Services | NVIDIA API: Nemotron (chat), Llama-Nemotron-Embed (embeddings), Llama-Nemotron-Rerank (reranking) |
| Pipeline | Apache PDFBox, Jsoup, POI, OpenCSV |
| ML | Python: XGBoost, LightGBM, scikit-learn, pandas, NumPy |

## Project Structure

```
TradeBridge/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/trade/
│   │   ├── config/            # Security, datasource configs
│   │   ├── controller/        # REST controllers
│   │   ├── entity/            # JPA entities
│   │   ├── regulatory/        # HS classification, RAG, export analysis
│   │   ├── repository/        # Data access
│   │   ├── security/          # JWT auth
│   │   └── service/           # Business logic
│   └── src/main/resources/
│       └── ml/                # Static ML ranking CSV
├── frontend/                   # React SPA
│   └── src/
│       ├── components/        # Reusable UI components
│       ├── pages/             # Route pages (Exporter, Logistics)
│       └── services/          # API client layer
├── pipeline/                   # ETL for regulatory data ingestion
├── 05(data)/                   # ML workspace (data not in repo)
│   └── src/                   # Python scripts for model training
│       ├── build_dataset.py
│       ├── rolling_origin_eval.py
│       └── make_paper_figure.py
├── TRADE_RANKING_PAPER_IEEE.tex
├── CBEC_AI_CONFERENCE_PAPER.tex
└── references.bib
```

## Getting Started

### Prerequisites
- Java 21+
- Node.js 18+
- MySQL 8
- Maven 3.9+
- Python 3.10+ (for ML pipeline only)

### Backend Setup
```bash
cd backend
# Copy and configure environment
cp .env.example .env
# Edit .env with your database credentials and NVIDIA API key

# Build and run
mvn clean install -DskipTests
mvn spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

### ML Pipeline (offline, optional)
```bash
cd "05(data)"
pip install xgboost lightgbm scikit-learn pandas numpy scipy matplotlib
python src/build_dataset.py
python src/rolling_origin_eval.py
```

## Data Sources

- **CEPII BACI HS17** (V202601): Bilateral trade flows 2017–2024
- **CEPII Gravity** (V202211): Distance, borders, FTAs, colonial links
- **World Bank WDI**: GDP, population, entry cost indicators

## Research Papers

This repository accompanies two IEEE conference papers:

1. **"Learning to Rank for Export Destination Selection: A Rolling-Origin Evaluation"** — focused on the ML ranking methodology and temporal validation
2. **"CBEC-AI: A Context-Aware AI-Driven Platform for Cross-Border Export Intelligence"** — focused on the full system architecture and all three modules

## License

This project is developed as an academic research project at B V Raju Institute of Technology.

## Authors

Murala Purnachandra Rao, Nethi Harini, Raya Shashidar Reddy, Patha Suhas  
Department of Computer Science and Engineering (Data Science)  
B V Raju Institute of Technology, Narsapur, Medak, Telangana, India

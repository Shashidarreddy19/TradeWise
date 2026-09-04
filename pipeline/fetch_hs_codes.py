"""
fetch_hs_codes.py — UN Comtrade HS Code Fetcher & DB Populator
===============================================================

Fetches the complete HS 2022 (H6) classification from UN Comtrade's reference API
and populates the TradeData.hs_master table for all project countries.

This script:
1. Downloads the full HS 2022 classification tree (chapters → headings → HS6 codes)
2. Maps each code to the project's target countries
3. Inserts into TradeData.hs_master with proper hierarchy (chapter, heading, hs6)
4. Assigns categories based on chapter-to-category mapping
5. Handles duplicates via ON DUPLICATE KEY UPDATE

Usage:
    python fetch_hs_codes.py [--dry-run] [--countries INDIA,USA,...] [--chapters 10,27,...]

Environment variables (or edit defaults below):
    DB_HOST       = localhost
    DB_PORT       = 3306
    DB_NAME       = TradeData
    DB_USER       = root
    DB_PASSWORD   = root
"""

import os
import sys
import json
import time
import hashlib
import argparse
from datetime import datetime

import urllib3
import mysql.connector
from mysql.connector import Error as MySQLError

# ═══════════════════════════════════════════════════════════════════════════════
# CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════

DB_CONFIG = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'port': int(os.environ.get('DB_PORT', '3306')),
    'database': os.environ.get('DB_NAME', 'TradeData'),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASSWORD', 'root'),
    'charset': 'utf8mb4',
    'autocommit': False,
}

# UN Comtrade HS 2022 reference endpoint
HS_2022_URL = 'https://comtradeapi.un.org/files/v1/app/reference/H6.json'

# Project countries with their customs territory codes
PROJECT_COUNTRIES = {
    'India': {'territory': 'IN', 'nomenclature': 'ITC_HS'},
    'United States': {'territory': 'US', 'nomenclature': 'HTS'},
    'United Kingdom': {'territory': 'GB', 'nomenclature': 'UK_TRADE_TARIFF'},
    'Germany': {'territory': 'DE', 'nomenclature': 'CN'},
    'Netherlands': {'territory': 'NL', 'nomenclature': 'CN'},
    'United Arab Emirates': {'territory': 'AE', 'nomenclature': 'GCC_HS'},
    'Saudi Arabia': {'territory': 'SA', 'nomenclature': 'GCC_HS'},
    'Singapore': {'territory': 'SG', 'nomenclature': 'STCCED'},
    'Bangladesh': {'territory': 'BD', 'nomenclature': 'HS_BD'},
    'China': {'territory': 'CN_COUNTRY', 'nomenclature': 'HS_CN'},
    'Hong Kong': {'territory': 'HK', 'nomenclature': 'HKHS'},
}

# Chapter → Category mapping (matches HsClassificationService.CATEGORY_CHAPTERS)
CHAPTER_TO_CATEGORY = {
    '01': 'Agricultural Products', '02': 'Food Products', '03': 'Marine Products',
    '04': 'Food Products', '05': 'Agricultural Products', '06': 'Agricultural Products',
    '07': 'Agricultural Products', '08': 'Agricultural Products', '09': 'Spices',
    '10': 'Agricultural Products', '11': 'Food Products', '12': 'Agricultural Products',
    '13': 'Agricultural Products', '14': 'Agricultural Products', '15': 'Food Products',
    '16': 'Processed Foods', '17': 'Processed Foods', '18': 'Processed Foods',
    '19': 'Processed Foods', '20': 'Processed Foods', '21': 'Processed Foods',
    '22': 'Processed Foods', '23': 'Food Products', '24': 'Agricultural Products',
    '25': 'Chemicals', '26': 'Chemicals', '27': 'Chemicals',
    '28': 'Chemicals', '29': 'Chemicals', '30': 'Pharmaceuticals',
    '31': 'Chemicals', '32': 'Chemicals', '33': 'Cosmetics & Personal Care',
    '34': 'Chemicals', '35': 'Chemicals', '36': 'Chemicals',
    '37': 'Chemicals', '38': 'Chemicals', '39': 'Plastics & Rubber',
    '40': 'Plastics & Rubber', '41': 'Leather Products', '42': 'Leather Products',
    '43': 'Leather Products', '44': 'Furniture & Wood', '45': 'Furniture & Wood',
    '46': 'Furniture & Wood', '47': 'Furniture & Wood', '48': 'Furniture & Wood',
    '49': 'Furniture & Wood', '50': 'Textiles', '51': 'Textiles',
    '52': 'Textiles', '53': 'Textiles', '54': 'Textiles',
    '55': 'Textiles', '56': 'Textiles', '57': 'Textiles',
    '58': 'Textiles', '59': 'Textiles', '60': 'Textiles',
    '61': 'Apparel & Garments', '62': 'Apparel & Garments', '63': 'Textiles',
    '64': 'Footwear', '65': 'Apparel & Garments', '66': 'Apparel & Garments',
    '67': 'Apparel & Garments', '68': 'Engineering Goods', '69': 'Ceramics & Pottery',
    '70': 'Engineering Goods', '71': 'Jewellery & Gems', '72': 'Engineering Goods',
    '73': 'Engineering Goods', '74': 'Engineering Goods', '75': 'Engineering Goods',
    '76': 'Engineering Goods', '77': 'Engineering Goods', '78': 'Engineering Goods',
    '79': 'Engineering Goods', '80': 'Engineering Goods', '81': 'Engineering Goods',
    '82': 'Engineering Goods', '83': 'Engineering Goods', '84': 'Machinery',
    '85': 'Electronics', '86': 'Engineering Goods', '87': 'Automotive Components',
    '88': 'Engineering Goods', '89': 'Engineering Goods', '90': 'Electronics',
    '91': 'Electronics', '92': 'Electronics', '93': 'Engineering Goods',
    '94': 'Furniture & Wood', '95': 'Others', '96': 'Others',
    '97': 'Others',
}

# Chapters relevant to the project (India's top export chapters used in ML model)
PROJECT_CHAPTERS = [
    '10', '27', '29', '30', '61', '62', '71', '72', '84', '85', '87',
    # Additional supporting chapters for classification coverage
    '01', '02', '03', '04', '05', '06', '07', '08', '09', '11', '12',
    '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23',
    '24', '25', '26', '28', '31', '32', '33', '34', '35', '36', '37',
    '38', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48',
    '49', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59',
    '60', '63', '64', '65', '66', '67', '68', '69', '70', '73', '74',
    '75', '76', '78', '79', '80', '81', '82', '83', '86', '88', '89',
    '90', '91', '92', '93', '94', '95', '96', '97',
]


# ═══════════════════════════════════════════════════════════════════════════════
# FETCH HS DATA FROM COMTRADE
# ═══════════════════════════════════════════════════════════════════════════════

def fetch_hs_reference():
    """Download the full HS 2022 classification tree from UN Comtrade."""
    print(f"[FETCH] Downloading HS 2022 reference from: {HS_2022_URL}")
    http = urllib3.PoolManager()
    
    retries = 3
    for attempt in range(retries):
        try:
            resp = http.request("GET", HS_2022_URL, timeout=120)
            if resp.status == 200:
                data = json.loads(resp.data)
                results = data.get('results', [])
                print(f"[FETCH] Successfully downloaded {len(results)} HS code entries.")
                return results
            else:
                print(f"[FETCH] HTTP {resp.status} — retrying ({attempt+1}/{retries})...")
                time.sleep(5)
        except Exception as e:
            print(f"[FETCH] Error: {e} — retrying ({attempt+1}/{retries})...")
            time.sleep(5)
    
    print("[FETCH] FAILED to download HS reference data after all retries.")
    sys.exit(1)


def parse_hs_entries(raw_entries, chapters_filter=None):
    """
    Parse raw Comtrade HS reference into structured records.
    
    Each entry has: id, text, parent, isLeaf, aggrlevel, standardUnitAbbr
    - aggrlevel 2 = chapter (2-digit)
    - aggrlevel 4 = heading (4-digit)
    - aggrlevel 6 = subheading (6-digit, HS6 level)
    
    We want ALL levels for the hs_master table.
    """
    records = []
    
    for entry in raw_entries:
        code = entry.get('id', '').strip()
        text = entry.get('text', '').strip()
        aggr_level = entry.get('aggrlevel', 0)
        is_leaf = str(entry.get('isLeaf', '0'))
        unit_abbr = entry.get('standardUnitAbbr', '')
        
        # Skip TOTAL and AG aggregation entries
        if code in ('TOTAL', 'ALL', '') or not code.isdigit():
            continue
        
        # Only include chapters, headings, and HS6 codes (2, 4, 6 digit)
        if aggr_level not in (2, 4, 6):
            continue
        
        # Parse description from "CODE - Description" format
        description = text
        if ' - ' in text:
            description = text.split(' - ', 1)[1].strip()
        
        # Derive hierarchy
        chapter = code[:2] if len(code) >= 2 else code.ljust(2, '0')
        heading = code[:4] if len(code) >= 4 else code.ljust(4, '0')
        hs6 = code[:6] if len(code) >= 6 else code.ljust(6, '0')
        
        # Filter by chapters if specified
        if chapters_filter and chapter not in chapters_filter:
            continue
        
        # Determine unit
        unit = unit_abbr if unit_abbr and unit_abbr != 'n/a' else None
        
        # Get category from chapter
        category = CHAPTER_TO_CATEGORY.get(chapter, 'Others')
        
        records.append({
            'national_code': code,
            'chapter': chapter,
            'heading': heading,
            'hs6': hs6,
            'code_length': len(code),
            'official_description': description,
            'unit': unit,
            'category': category,
            'is_leaf': is_leaf == '1',
        })
    
    print(f"[PARSE] Parsed {len(records)} HS code records "
          f"(Chapters: {len(set(r['chapter'] for r in records))}, "
          f"Headings: {len([r for r in records if r['code_length'] == 4])}, "
          f"HS6: {len([r for r in records if r['code_length'] == 6])})")
    
    return records


# ═══════════════════════════════════════════════════════════════════════════════
# DATABASE OPERATIONS
# ═══════════════════════════════════════════════════════════════════════════════

def get_db_connection():
    """Create MySQL connection to TradeData database."""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        print(f"[DB] Connected to {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
        return conn
    except MySQLError as e:
        print(f"[DB] Connection failed: {e}")
        sys.exit(1)


def ensure_table_exists(cursor):
    """Create hs_master table if it doesn't exist."""
    ddl = """
    CREATE TABLE IF NOT EXISTS hs_master (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        customs_territory VARCHAR(50) NOT NULL,
        country VARCHAR(100) NOT NULL,
        chapter VARCHAR(2) NOT NULL,
        heading VARCHAR(4) NOT NULL,
        hs6 VARCHAR(6) NOT NULL,
        national_code VARCHAR(50) NOT NULL,
        code_length INT NOT NULL,
        nomenclature_type VARCHAR(50) NOT NULL,
        official_description TEXT NOT NULL,
        unit VARCHAR(50),
        dataset_version VARCHAR(50) NOT NULL,
        is_current TINYINT(1) DEFAULT 1,
        is_active TINYINT(1) DEFAULT 1,
        record_hash VARCHAR(64),
        remarks VARCHAR(255),
        effective_from DATETIME,
        effective_to DATETIME,
        source_id BIGINT,
        last_verified DATETIME NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        category VARCHAR(100) NOT NULL DEFAULT 'Others',
        UNIQUE KEY uk_country_territory_code_version (country, customs_territory, national_code, dataset_version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
    cursor.execute(ddl)
    
    # Ensure FULLTEXT index on official_description
    try:
        cursor.execute("""
            SELECT COUNT(*) FROM information_schema.STATISTICS 
            WHERE TABLE_SCHEMA = %s AND TABLE_NAME = 'hs_master' 
            AND INDEX_NAME = 'ft_official_description'
        """, (DB_CONFIG['database'],))
        if cursor.fetchone()[0] == 0:
            cursor.execute("ALTER TABLE hs_master ADD FULLTEXT INDEX ft_official_description (official_description)")
            print("[DB] Created FULLTEXT index on hs_master.official_description")
    except MySQLError as e:
        print(f"[DB] FULLTEXT index check/create warning: {e}")
    
    print("[DB] Table hs_master verified/created.")


def compute_record_hash(country, territory, national_code, description):
    """Deterministic hash for deduplication."""
    content = f"{country}|{territory}|{national_code}|{description}"
    return hashlib.sha256(content.encode('utf-8')).hexdigest()


def insert_hs_records(cursor, records, country, territory, nomenclature, dataset_version, dry_run=False):
    """
    Insert HS records into hs_master for a specific country.
    Uses INSERT ... ON DUPLICATE KEY UPDATE for idempotency.
    """
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    insert_sql = """
    INSERT INTO hs_master 
        (customs_territory, country, chapter, heading, hs6, national_code, 
         code_length, nomenclature_type, official_description, unit,
         dataset_version, is_current, is_active, record_hash, 
         last_verified, category)
    VALUES 
        (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 1, 1, %s, %s, %s)
    ON DUPLICATE KEY UPDATE
        official_description = VALUES(official_description),
        unit = VALUES(unit),
        category = VALUES(category),
        is_current = 1,
        is_active = 1,
        last_verified = VALUES(last_verified),
        updated_at = NOW()
    """
    
    batch = []
    inserted = 0
    updated = 0
    
    for record in records:
        record_hash = compute_record_hash(
            country, territory, record['national_code'], record['official_description']
        )
        
        params = (
            territory,
            country,
            record['chapter'],
            record['heading'],
            record['hs6'],
            record['national_code'],
            record['code_length'],
            nomenclature,
            record['official_description'],
            record['unit'],
            dataset_version,
            record_hash,
            now,
            record['category'],
        )
        batch.append(params)
        
        # Execute in batches of 500
        if len(batch) >= 500:
            if not dry_run:
                cursor.executemany(insert_sql, batch)
                inserted += cursor.rowcount
            else:
                inserted += len(batch)
            batch = []
    
    # Flush remaining
    if batch:
        if not dry_run:
            cursor.executemany(insert_sql, batch)
            inserted += cursor.rowcount
        else:
            inserted += len(batch)
    
    return inserted


# ═══════════════════════════════════════════════════════════════════════════════
# MAIN PIPELINE
# ═══════════════════════════════════════════════════════════════════════════════

def main():
    parser = argparse.ArgumentParser(description='Fetch HS codes from UN Comtrade and populate TradeData.hs_master')
    parser.add_argument('--dry-run', action='store_true', help='Preview without writing to database')
    parser.add_argument('--countries', type=str, default=None,
                        help='Comma-separated list of countries (default: all project countries)')
    parser.add_argument('--chapters', type=str, default=None,
                        help='Comma-separated list of chapters to fetch (default: all 97 chapters)')
    parser.add_argument('--dataset-version', type=str, default='HS_2022_COMTRADE',
                        help='Dataset version identifier (default: HS_2022_COMTRADE)')
    args = parser.parse_args()
    
    print("=" * 70)
    print("  UN COMTRADE HS CODE FETCHER — TradeData.hs_master Populator")
    print("=" * 70)
    print(f"  Mode: {'DRY RUN (no DB writes)' if args.dry_run else 'LIVE (writing to DB)'}")
    print(f"  Dataset version: {args.dataset_version}")
    print(f"  Database: {DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")
    print("=" * 70)
    
    # Determine which countries to process
    if args.countries:
        country_names = [c.strip() for c in args.countries.split(',')]
        countries = {k: v for k, v in PROJECT_COUNTRIES.items() if k in country_names}
        if not countries:
            print(f"[ERROR] No valid countries found in: {args.countries}")
            print(f"  Available: {', '.join(PROJECT_COUNTRIES.keys())}")
            sys.exit(1)
    else:
        countries = PROJECT_COUNTRIES
    
    # Determine chapters filter
    chapters_filter = None
    if args.chapters:
        chapters_filter = [c.strip().zfill(2) for c in args.chapters.split(',')]
    
    print(f"\n[CONFIG] Countries: {', '.join(countries.keys())}")
    print(f"[CONFIG] Chapters: {'ALL (97)' if not chapters_filter else ', '.join(chapters_filter)}")
    
    # Step 1: Fetch HS reference data from Comtrade
    print(f"\n{'─' * 70}")
    raw_entries = fetch_hs_reference()
    
    # Step 2: Parse into structured records
    hs_records = parse_hs_entries(raw_entries, chapters_filter)
    
    if not hs_records:
        print("[ERROR] No records parsed. Exiting.")
        sys.exit(1)
    
    # Step 3: Connect to database and insert for each country
    print(f"\n{'─' * 70}")
    
    if args.dry_run:
        print("[DRY RUN] Skipping database connection.")
        for country, meta in countries.items():
            print(f"\n  [{country}] Would insert {len(hs_records)} records "
                  f"(territory={meta['territory']}, type={meta['nomenclature']})")
        
        # Print sample records
        print(f"\n{'─' * 70}")
        print("[DRY RUN] Sample records (first 5):")
        for i, rec in enumerate(hs_records[:5]):
            print(f"  {i+1}. {rec['national_code']} | Ch={rec['chapter']} "
                  f"| Hd={rec['heading']} | HS6={rec['hs6']} "
                  f"| {rec['category']}")
            print(f"     {rec['official_description'][:80]}...")
        
        total_would_insert = len(hs_records) * len(countries)
        print(f"\n[DRY RUN] Total records that would be inserted: {total_would_insert}")
        print("[DRY RUN] Complete. Run without --dry-run to write to database.")
        return
    
    # Live mode — connect and insert
    conn = get_db_connection()
    cursor = conn.cursor()
    
    try:
        ensure_table_exists(cursor)
        conn.commit()
        
        total_inserted = 0
        
        for country, meta in countries.items():
            territory = meta['territory']
            nomenclature = meta['nomenclature']
            
            print(f"\n  [INSERT] {country} ({territory}) — {len(hs_records)} records...")
            
            count = insert_hs_records(
                cursor, hs_records, country, territory, nomenclature,
                args.dataset_version, dry_run=False
            )
            conn.commit()
            total_inserted += count
            print(f"  [INSERT] {country}: {count} rows affected (inserts + updates)")
        
        print(f"\n{'═' * 70}")
        print(f"  COMPLETE: {total_inserted} total rows affected across {len(countries)} countries")
        print(f"{'═' * 70}")
        
        # Print summary
        cursor.execute("""
            SELECT country, customs_territory, COUNT(*) as total,
                   SUM(CASE WHEN code_length = 6 THEN 1 ELSE 0 END) as hs6_count,
                   SUM(CASE WHEN code_length = 4 THEN 1 ELSE 0 END) as heading_count,
                   SUM(CASE WHEN code_length = 2 THEN 1 ELSE 0 END) as chapter_count
            FROM hs_master 
            WHERE is_current = 1 AND dataset_version = %s
            GROUP BY country, customs_territory
            ORDER BY country
        """, (args.dataset_version,))
        
        print(f"\n  {'Country':<25} {'Territory':<6} {'Total':<8} {'HS6':<8} {'Headings':<10} {'Chapters':<8}")
        print(f"  {'─'*25} {'─'*6} {'─'*8} {'─'*8} {'─'*10} {'─'*8}")
        for row in cursor.fetchall():
            print(f"  {row[0]:<25} {row[1]:<6} {row[2]:<8} {row[3]:<8} {row[4]:<10} {row[5]:<8}")
        
    except MySQLError as e:
        conn.rollback()
        print(f"[DB ERROR] {e}")
        sys.exit(1)
    finally:
        cursor.close()
        conn.close()
        print("\n[DB] Connection closed.")


if __name__ == '__main__':
    main()

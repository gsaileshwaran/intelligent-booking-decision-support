#!/usr/bin/env python3
"""
PVK CINEMAS -- Phase 1A Demo Data Reset Script (Python/PyMySQL)
============================================================
Dataset Version : 2026.10
Deterministic Seed: PVK-PHASE1-SEED-42
Phase           : 1A (Physical Cinema Ecosystem Realism)

PURPOSE:
  Performs a dependency-aware, safe reset of PVK Cinemas demo data.
  - NEVER drops the database or tables.
  - NEVER touches flyway_schema_history.
  - Cleans categories B/C/D (operational, transactional, ephemeral).
  - Restores category A (master/reference) to the deterministic Phase 1A state:
    * Exactly 5 cities
    * Exactly 25 theatres (5 per city)
    * Realistically varied screen counts (3-8 screens per theatre, 127 total)
    * Realistically varied capacities (64-240 seats per screen, 16,386 total)
    * Premium formats (IMAX, 4DX) assigned selectively to flagship venues
    * Geometrically sound seat layouts compatible with AI recommendation engine
    * Stable catalogue reference data (15 movies)
  - Idempotent: safe to run repeatedly.

USAGE:
  python phase1_reset.py [--validate-only] [--env-file ../.env]

EXIT CODES:
  0 = success
  1 = validation failure
  2 = database connection error
  3 = data integrity error
"""

import sys
import os
import argparse
import datetime

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

# ---------------------------------------------------------------------------
# CONFIGURATION
# ---------------------------------------------------------------------------
DATASET_VERSION = "2026.10"
DATASET_SEED    = "PVK-PHASE1-SEED-42"
PHASE           = "1A"

DEMO_ADMIN    = "admin@pvkcinemas.com"
DEMO_MANAGER  = "manager@pvkcinemas.com"
DEMO_CUSTOMER = "customer@pvkcinemas.com"

# BCrypt hash of "Password123!" (cost=10, same hash used across all existing migrations)
PASSWORD_HASH = "$2a$10$iMQorm2sO/mG5gw80k.wxO5iDlyYL5azgoHtmF1S7cWP7GbqYs9Be"

MANAGER_THEATRE_CODE = "DEMO_CHN_CINEPLEX"

EXPECTED_CITIES            = 5
EXPECTED_THEATRES          = 25
EXPECTED_THEATRES_PER_CITY = 5
EXPECTED_SCREENS           = 127
EXPECTED_SEATS_TOTAL       = 16386
EXPECTED_SEATS_MIN         = 15000
EXPECTED_ROLES             = ["ROLE_SUPER_ADMIN", "ROLE_THEATRE_MANAGER", "ROLE_CUSTOMER"]
EXPECTED_DEMO_USERS        = [DEMO_ADMIN, DEMO_MANAGER, DEMO_CUSTOMER]

# ---------------------------------------------------------------------------
# CANONICAL MASTER DATA
# ---------------------------------------------------------------------------

CITIES = [
    (1, "Chennai",   "Tamil Nadu",  "IN"),
    (2, "Bengaluru", "Karnataka",   "IN"),
    (3, "Hyderabad", "Telangana",   "IN"),
    (4, "Mumbai",    "Maharashtra", "IN"),
    (5, "Delhi",     "Delhi",       "IN"),
]

THEATRES = [
    # Chennai (city_id=1), theatre_id 1-5
    (1, 1, "DEMO_CHN_CINEPLEX", "PVK Cineplex Chennai",  "Express Avenue Mall, Whites Road, Royapettah", "Level 3, Theatre Block",     "600014",  13.064220, 80.268080, "ACTIVE"),
    (2, 1, "DEMO_CHN_INOX",     "PVK INOX Chennai",      "Phoenix MarketCity, Velachery Main Road",      "Velachery, Chennai",         "600042",  12.980310, 80.218780, "ACTIVE"),
    (3, 1, "DEMO_CHN_PVR",      "PVK PVR Chennai",       "VR Chennai Mall, Inner Ring Road, Anna Nagar", "Jawaharlal Nehru Road",      "600040",  13.085430, 80.201240, "ACTIVE"),
    (4, 1, "DEMO_CHN_AGS",      "PVK AGS Chennai",       "AGS Cinemas, OMR Navalur",                     "Rajiv Gandhi Salai",         "603103",  12.845620, 80.228910, "ACTIVE"),
    (5, 1, "DEMO_CHN_SPI",      "PVK SPI Chennai",       "Sathyam Complex, Royapettah",                  "8 Thiruvika Road",           "600014",  13.058320, 80.264170, "ACTIVE"),
    # Bengaluru (city_id=2), theatre_id 6-10
    (6,  2, "DEMO_BLR_CINEPLEX", "PVK Cineplex Bengaluru", "Forum Mall, Hosur Road, Koramangala",        "4th Block, Koramangala",     "560034",  12.935010, 77.616850, "ACTIVE"),
    (7,  2, "DEMO_BLR_INOX",     "PVK INOX Bengaluru",     "Garuda Mall, Magrath Road, Ashok Nagar",     "Near Residency Road",        "560025",  12.972910, 77.603550, "ACTIVE"),
    (8,  2, "DEMO_BLR_PVR",      "PVK PVR Bengaluru",      "Orion Mall, Dr Rajkumar Road, Rajajinagar",  "Brigade Gateway",            "560055",  13.011240, 77.555230, "ACTIVE"),
    (9,  2, "DEMO_BLR_AGS",      "PVK AGS Bengaluru",      "Nexus Shantiniketan, Whitefield Main Road",  "Hoodi, Whitefield",          "560048",  12.989210, 77.728910, "ACTIVE"),
    (10, 2, "DEMO_BLR_SPI",      "PVK SPI Bengaluru",      "Lulu Global Malls, Gopalapura, Binnypet",    "Magadi Road",                "560023",  12.978930, 77.564120, "ACTIVE"),
    # Hyderabad (city_id=3), theatre_id 11-15
    (11, 3, "DEMO_HYD_CINEPLEX", "PVK Cineplex Hyderabad", "Sarath City Capital Mall, Kondapur",         "Gachibowli-Miyapur Rd",      "500084",  17.456810, 78.364210, "ACTIVE"),
    (12, 3, "DEMO_HYD_INOX",     "PVK INOX Hyderabad",     "GVK One Mall, Rd Number 1, Banjara Hills",   "Balapur Basheerbagh Rd",     "500034",  17.419240, 78.448510, "ACTIVE"),
    (13, 3, "DEMO_HYD_PVR",      "PVK PVR Hyderabad",      "Next Galleria Mall, Hitec City",             "Madhapur",                   "500081",  17.449830, 78.381240, "ACTIVE"),
    (14, 3, "DEMO_HYD_AMB",      "PVK AMB Hyderabad",      "AMB Cinemas, Botanical Garden Road, Kondapur","Hi-Tech City",             "500084",  17.450210, 78.373610, "ACTIVE"),
    (15, 3, "DEMO_HYD_PRASADS",  "PVK Prasads Hyderabad",  "Prasads Multiplex, NTR Marg, Central",       "Khairatabad",                "500063",  17.413210, 78.468240, "ACTIVE"),
    # Mumbai (city_id=4), theatre_id 16-20
    (16, 4, "DEMO_BOM_CINEPLEX", "PVK Cineplex Mumbai",    "Phoenix Palladium, Senapati Bapat Marg, Lower Parel","South Mumbai",       "400013",  18.995320, 72.824810, "ACTIVE"),
    (17, 4, "DEMO_BOM_INOX",     "PVK INOX Mumbai",        "Inorbit Mall, Link Road, Malad West",         "Suburban Mumbai",           "400064",  19.173820, 72.835120, "ACTIVE"),
    (18, 4, "DEMO_BOM_PVR",      "PVK PVR Mumbai",         "PVR Dynamix Mall, JVPD Scheme, Juhu",         "Vile Parle West",           "400049",  19.107410, 72.826720, "ACTIVE"),
    (19, 4, "DEMO_BOM_AGS",      "PVK AGS Mumbai",         "R City Mall, LBS Road, Ghatkopar West",       "Central Suburbs",           "400086",  19.099430, 72.916820, "ACTIVE"),
    (20, 4, "DEMO_BOM_METRO",    "PVK Metro Mumbai",       "Metro INOX Cinemas, MG Road, Dhobi Talao",    "Marine Lines",              "400020",  18.941210, 72.829140, "ACTIVE"),
    # Delhi (city_id=5), theatre_id 21-25
    (21, 5, "DEMO_DEL_CINEPLEX", "PVK Cineplex Delhi",     "Select CITYWALK, Saket District Centre",      "Saket, South Delhi",        "110017",  28.528410, 77.218920, "ACTIVE"),
    (22, 5, "DEMO_DEL_INOX",     "PVK INOX Delhi",         "Nehru Place Epicuria, Metro Station",         "Nehru Place",               "110019",  28.551230, 77.251410, "ACTIVE"),
    (23, 5, "DEMO_DEL_PVR",      "PVK PVR Delhi",          "PVR Plaza, Connaught Circus, H-Block",        "Connaught Place",           "110001",  28.632940, 77.219810, "ACTIVE"),
    (24, 5, "DEMO_DEL_AGS",      "PVK AGS Delhi",          "Pacific Mall, Tagore Garden, Najafgarh Road", "West Delhi",                "110027",  28.642130, 77.108420, "ACTIVE"),
    (25, 5, "DEMO_DEL_METRO",    "PVK Metro Delhi",        "V3S East Centre Mall, Vikas Marg, Nirman Vihar","Laxmi Nagar",             "110092",  28.636410, 77.284520, "ACTIVE"),
]

# ---------------------------------------------------------------------------
# PHASE 1A: REALISTIC AUDITORIUM ARCHETYPES
# ---------------------------------------------------------------------------
# Each archetype defines:
#   name_template: descriptive screen name
#   format_code: presentation format (2D, 3D, IMAX, 4DX)
#   audio_code: audio format (DOLBY_5_1, DOLBY_7_1, DOLBY_ATMOS, IMAX_12CH)
#   rows: total rows (8, 10, or 12)
#   cols: total columns (8, 11, 13, 15, or 20)
#   aisles: list of column numbers after which an aisle appears
#   row_rules: [(start_row, end_row, default_seat_type, pricing_zone, [accessible_cols])]

ARCHETYPES = {
    'IMAX_GRAND': {
        'name_template': 'Screen {idx} - IMAX Laser Experience',
        'format_code': 'IMAX', 'audio_code': 'IMAX_12CH',
        'rows': 12, 'cols': 20, 'seats': 240, 'aisles': [5, 15],
        'row_rules': [
            (1, 3, 'STANDARD', 'VALUE', []),
            (4, 5, 'STANDARD', 'STANDARD', []),
            (6, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 11, 'RECLINER', 'PREMIUM', []),
            (12, 12, 'RECLINER', 'PREMIUM', [1, 2, 19, 20]),
        ]
    },
    'DOLBY_GRAND': {
        'name_template': 'Screen {idx} - Dolby Atmos Cinema',
        'format_code': '2D', 'audio_code': 'DOLBY_ATMOS',
        'rows': 12, 'cols': 15, 'seats': 180, 'aisles': [4, 11],
        'row_rules': [
            (1, 2, 'STANDARD', 'VALUE', []),
            (3, 5, 'STANDARD', 'STANDARD', []),
            (6, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 11, 'RECLINER', 'PREMIUM', []),
            (12, 12, 'RECLINER', 'PREMIUM', [1, 15]),
        ]
    },
    'CLASSIC_150': {
        'name_template': 'Screen {idx} - Classic Auditorium',
        'format_code': '2D', 'audio_code': 'DOLBY_5_1',
        'rows': 10, 'cols': 15, 'seats': 150, 'aisles': [4, 11],
        'row_rules': [
            (1, 3, 'STANDARD', 'VALUE', []),
            (4, 7, 'STANDARD', 'STANDARD', []),
            (8, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 10, 'PREMIUM', 'PREMIUM', [1, 15]),
        ]
    },
    'STANDARD_130': {
        'name_template': 'Screen {idx} - Standard 2D Auditorium',
        'format_code': '2D', 'audio_code': 'DOLBY_7_1',
        'rows': 10, 'cols': 13, 'seats': 130, 'aisles': [3, 10],
        'row_rules': [
            (1, 3, 'STANDARD', 'VALUE', []),
            (4, 6, 'STANDARD', 'STANDARD', []),
            (7, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 10, 'RECLINER', 'PREMIUM', [1, 13]),
        ]
    },
    'DIGITAL_3D_130': {
        'name_template': 'Screen {idx} - Digital 3D Auditorium',
        'format_code': '3D', 'audio_code': 'DOLBY_7_1',
        'rows': 10, 'cols': 13, 'seats': 130, 'aisles': [3, 10],
        'row_rules': [
            (1, 3, 'STANDARD', 'VALUE', []),
            (4, 6, 'STANDARD', 'STANDARD', []),
            (7, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 10, 'RECLINER', 'PREMIUM', [1, 13]),
        ]
    },
    'MEDIUM_110': {
        'name_template': 'Screen {idx} - Medium 2D Auditorium',
        'format_code': '2D', 'audio_code': 'DOLBY_7_1',
        'rows': 10, 'cols': 11, 'seats': 110, 'aisles': [3, 8],
        'row_rules': [
            (1, 3, 'STANDARD', 'VALUE', []),
            (4, 7, 'STANDARD', 'STANDARD', []),
            (8, 9, 'PREMIUM', 'PREMIUM', []),
            (10, 10, 'RECLINER', 'PREMIUM', [1, 11]),
        ]
    },
    'MOTION_4DX_88': {
        'name_template': 'Screen {idx} - 4DX Dynamic Motion',
        'format_code': '4DX', 'audio_code': 'DOLBY_7_1',
        'rows': 8, 'cols': 11, 'seats': 88, 'aisles': [3, 8],
        'row_rules': [
            (1, 2, 'STANDARD', 'VALUE', []),
            (3, 5, 'PREMIUM', 'STANDARD', []),
            (6, 7, 'PREMIUM', 'PREMIUM', []),
            (8, 8, 'RECLINER', 'PREMIUM', [1, 11]),
        ]
    },
    'BOUTIQUE_64': {
        'name_template': 'Screen {idx} - Boutique VIP Lounge',
        'format_code': '2D', 'audio_code': 'DOLBY_ATMOS',
        'rows': 8, 'cols': 8, 'seats': 64, 'aisles': [4],
        'row_rules': [
            (1, 2, 'PREMIUM', 'STANDARD', []),
            (3, 7, 'RECLINER', 'PREMIUM', []),
            (8, 8, 'RECLINER', 'PREMIUM', [1, 8]),
        ]
    },
}

# ---------------------------------------------------------------------------
# PHASE 1A: DETERMINISTIC THEATRE-TO-SCREEN DISTRIBUTION
# ---------------------------------------------------------------------------
# Screen counts per theatre:
#   3 screens: 5 theatres (boutique / neighborhood)
#   4 screens: 5 theatres (medium)
#   5 screens: 5 theatres (standard multiplex)
#   6 screens: 5 theatres (large regional multiplex)
#   7 screens: 3 theatres (flagship downtown cineplex)
#   8 screens: 2 theatres (megaplex destination hub)
# Total: 127 screens, 16,386 seats

THEATRE_SCREENS = {
    # Chennai (city 1) - Total: 25 screens
    "DEMO_CHN_CINEPLEX": ["IMAX_GRAND", "DOLBY_GRAND", "MOTION_4DX_88", "STANDARD_130", "DIGITAL_3D_130", "MEDIUM_110", "BOUTIQUE_64"], # 7
    "DEMO_CHN_INOX":     ["DOLBY_GRAND", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                   # 6
    "DEMO_CHN_PVR":      ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                      # 5
    "DEMO_CHN_AGS":      ["STANDARD_130", "CLASSIC_150", "MEDIUM_110", "MEDIUM_110"],                                                      # 4
    "DEMO_CHN_SPI":      ["DOLBY_GRAND", "MEDIUM_110", "BOUTIQUE_64"],                                                                      # 3

    # Bengaluru (city 2) - Total: 26 screens
    "DEMO_BLR_CINEPLEX": ["IMAX_GRAND", "DOLBY_GRAND", "MOTION_4DX_88", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"], # 8
    "DEMO_BLR_INOX":     ["STANDARD_130", "CLASSIC_150", "MEDIUM_110", "MEDIUM_110"],                                                      # 4
    "DEMO_BLR_PVR":      ["DOLBY_GRAND", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                   # 6
    "DEMO_BLR_AGS":      ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                      # 5
    "DEMO_BLR_SPI":      ["STANDARD_130", "MEDIUM_110", "BOUTIQUE_64"],                                                                      # 3

    # Hyderabad (city 3) - Total: 29 screens
    "DEMO_HYD_CINEPLEX": ["IMAX_GRAND", "DOLBY_GRAND", "MOTION_4DX_88", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"], # 8
    "DEMO_HYD_INOX":     ["STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                                     # 4
    "DEMO_HYD_PVR":      ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                      # 5
    "DEMO_HYD_AMB":      ["DOLBY_GRAND", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                   # 6
    "DEMO_HYD_PRASADS":  ["IMAX_GRAND", "DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                       # 6

    # Mumbai (city 4) - Total: 25 screens
    "DEMO_BOM_CINEPLEX": ["IMAX_GRAND", "DOLBY_GRAND", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],       # 7
    "DEMO_BOM_INOX":     ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                      # 5
    "DEMO_BOM_PVR":      ["DOLBY_GRAND", "MEDIUM_110", "BOUTIQUE_64"],                                                                      # 3
    "DEMO_BOM_AGS":      ["MOTION_4DX_88", "DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                     # 6
    "DEMO_BOM_METRO":    ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110"],                                                     # 4

    # Delhi (city 5) - Total: 22 screens
    "DEMO_DEL_CINEPLEX": ["IMAX_GRAND", "DOLBY_GRAND", "STANDARD_130", "DIGITAL_3D_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],       # 7
    "DEMO_DEL_INOX":     ["STANDARD_130", "MEDIUM_110", "BOUTIQUE_64"],                                                                      # 3
    "DEMO_DEL_PVR":      ["DOLBY_GRAND", "MEDIUM_110", "BOUTIQUE_64"],                                                                      # 3
    "DEMO_DEL_AGS":      ["DOLBY_GRAND", "STANDARD_130", "CLASSIC_150", "MEDIUM_110", "BOUTIQUE_64"],                                      # 5
    "DEMO_DEL_METRO":    ["STANDARD_130", "CLASSIC_150", "MEDIUM_110", "MEDIUM_110"],                                                      # 4
}

DEMO_USERS = [
    # (user_id, first_name, last_name, email, phone, role_code, profile_type, employee_code, joining_date)
    (1, "Arun",     "Sharma", DEMO_ADMIN,    "+91-9800000001", "ROLE_SUPER_ADMIN",     "EMPLOYEE", "EMP-ADMIN-001", "2024-01-15"),
    (2, "Ravi",     "Kumar",  DEMO_MANAGER,  "+91-9800000002", "ROLE_THEATRE_MANAGER", "EMPLOYEE", "EMP-MGR-001",   "2024-03-01"),
    (3, "Kavya",    "Reddy",  DEMO_CUSTOMER, "+91-9800000003", "ROLE_CUSTOMER",        "CUSTOMER", None,            None),
]

# ---------------------------------------------------------------------------
# DB HELPER
# ---------------------------------------------------------------------------

def load_env(env_file: str):
    """Load a .env file into os.environ (simple key=value parser)."""
    if not os.path.exists(env_file):
        return
    with open(env_file) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, val = line.partition("=")
                val = val.strip().strip('"').strip("'")
                os.environ.setdefault(key.strip(), val)


def get_connection():
    """Return a PyMySQL connection using env variables."""
    try:
        import pymysql
    except ImportError:
        print("[ERROR] PyMySQL not installed. Run: pip install pymysql")
        sys.exit(2)

    host = os.environ.get("DB_HOST", "127.0.0.1")
    port = int(os.environ.get("DB_PORT", "3306"))
    user = os.environ.get("DB_USER", "root")
    password = os.environ.get("DB_PASSWORD", "root")
    database = os.environ.get("DB_NAME", "pvk_cinemas_db")

    try:
        conn = pymysql.connect(
            host=host, port=port, user=user, password=password,
            database=database, charset="utf8mb4",
            cursorclass=pymysql.cursors.DictCursor,
            autocommit=False
        )
        return conn
    except Exception as e:
        print(f"[ERROR] Cannot connect to MySQL: {e}")
        sys.exit(2)


# ---------------------------------------------------------------------------
# STEP 1: CLEAN OPERATIONAL / TRANSACTIONAL / EPHEMERAL DATA
# ---------------------------------------------------------------------------

def clean_operational_data(cur):
    """
    Remove operational (B), transactional (C), and ephemeral (D) data.
    Respects FK constraints via ordered deletion.
    NEVER touches Flyway schema history or schema objects.
    """
    print("  -> Cleaning ephemeral data (SEAT_HOLD)...")
    cur.execute("DELETE FROM SEAT_HOLD")

    print("  -> Cleaning transactional data (PAYMENT, BOOKING_SEAT, BOOKING)...")
    cur.execute("DELETE FROM PAYMENT")
    cur.execute("DELETE FROM BOOKING_SEAT")
    cur.execute("DELETE FROM BOOKING")

    print("  -> Cleaning operational data (SHOW_SEAT, SHOW)...")
    cur.execute("DELETE FROM SHOW_SEAT")
    cur.execute("DELETE FROM `SHOW`")

    print("  -> Cleaning search telemetry (SEARCH_RESULT, SEARCH_QUERY, SEARCH_INDEX_DOCUMENT)...")
    cur.execute("DELETE FROM SEARCH_RESULT")
    cur.execute("DELETE FROM SEARCH_QUERY")
    cur.execute("DELETE FROM SEARCH_INDEX_DOCUMENT")

    print("  -> Cleaning audit logs...")
    cur.execute("DELETE FROM AUDIT_LOG")


# ---------------------------------------------------------------------------
# STEP 2: CLEAN MASTER/REFERENCE DATA (except static reference tables)
# ---------------------------------------------------------------------------

def clean_master_data(cur):
    """
    Remove existing demo master data to allow clean re-insertion.
    Preserves ROLE, PERMISSION, ROLE_PERMISSION, SEAT_TYPE, GENRE, LANGUAGE,
    CERTIFICATION, PRESENTATION_FORMAT, AUDIO_FORMAT, and MOVIE catalogue.
    """
    print("  -> Cleaning physical seats...")
    cur.execute("DELETE FROM SEAT")

    print("  -> Cleaning screen capabilities...")
    cur.execute("DELETE FROM SCREEN_CAPABILITY")

    print("  -> Cleaning screens...")
    cur.execute("DELETE FROM SCREEN")

    print("  -> Cleaning employee-theatre assignments...")
    cur.execute("DELETE FROM EMPLOYEE_THEATRE")

    print("  -> Cleaning theatres...")
    cur.execute("DELETE FROM THEATRE")

    print("  -> Cleaning cities...")
    cur.execute("DELETE FROM CITY")

    print("  -> Cleaning demo users (profiles, roles, users)...")
    demo_emails_placeholders = ",".join(["%s"] * len(EXPECTED_DEMO_USERS))
    cur.execute(
        f"DELETE ep FROM EMPLOYEE_PROFILE ep JOIN USER u ON ep.user_id = u.user_id "
        f"WHERE u.email IN ({demo_emails_placeholders})",
        EXPECTED_DEMO_USERS
    )
    cur.execute(
        f"DELETE cp FROM CUSTOMER_PROFILE cp JOIN USER u ON cp.user_id = u.user_id "
        f"WHERE u.email IN ({demo_emails_placeholders})",
        EXPECTED_DEMO_USERS
    )
    cur.execute(
        f"DELETE ur FROM USER_ROLE ur JOIN USER u ON ur.user_id = u.user_id "
        f"WHERE u.email IN ({demo_emails_placeholders})",
        EXPECTED_DEMO_USERS
    )
    cur.execute(
        f"DELETE FROM USER WHERE email IN ({demo_emails_placeholders})",
        EXPECTED_DEMO_USERS
    )

    # Reset AUTO_INCREMENT for deterministic IDs
    for table, ai_val in [
        ("CITY", 1), ("THEATRE", 1), ("SCREEN", 1),
        ("SCREEN_CAPABILITY", 1), ("SEAT", 1),
        ("USER", 1), ("`SHOW`", 1), ("BOOKING", 1),
        ("PAYMENT", 1),
    ]:
        cur.execute(f"ALTER TABLE {table} AUTO_INCREMENT = {ai_val}")


# ---------------------------------------------------------------------------
# STEP 3: RESTORE CITIES
# ---------------------------------------------------------------------------

def restore_cities(cur):
    print("  -> Restoring 5 canonical cities...")
    cur.executemany(
        "INSERT INTO CITY (city_id, city_name, state_name, country_code) VALUES (%s, %s, %s, %s)",
        CITIES
    )


# ---------------------------------------------------------------------------
# STEP 4: RESTORE DEMO USERS
# ---------------------------------------------------------------------------

def restore_demo_users(cur):
    print("  -> Restoring 3 demo users...")

    cur.execute("SELECT role_code, role_id FROM ROLE WHERE role_code IN (%s,%s,%s)",
                ("ROLE_SUPER_ADMIN", "ROLE_THEATRE_MANAGER", "ROLE_CUSTOMER"))
    role_map = {row["role_code"]: row["role_id"] for row in cur.fetchall()}

    cur.execute("SELECT language_id FROM LANGUAGE WHERE language_code = %s", ("ta",))
    row = cur.fetchone()
    ta_lang_id = row["language_id"] if row else 1

    for uid, first, last, email, phone, role_code, profile_type, emp_code, joining_date in DEMO_USERS:
        cur.execute(
            "INSERT INTO USER (user_id, first_name, last_name, email, phone, password_hash, account_status) "
            "VALUES (%s, %s, %s, %s, %s, %s, 'ACTIVE')",
            (uid, first, last, email, phone, PASSWORD_HASH)
        )

        role_id = role_map.get(role_code)
        if role_id:
            cur.execute(
                "INSERT INTO USER_ROLE (user_id, role_id, status) VALUES (%s, %s, 'ACTIVE')",
                (uid, role_id)
            )

        if profile_type == "EMPLOYEE":
            cur.execute(
                "INSERT INTO EMPLOYEE_PROFILE (user_id, employee_code, joining_date, status) "
                "VALUES (%s, %s, %s, 'ACTIVE')",
                (uid, emp_code, joining_date)
            )
        elif profile_type == "CUSTOMER":
            cur.execute(
                "INSERT INTO CUSTOMER_PROFILE (user_id, preferred_language_id, date_of_birth) "
                "VALUES (%s, %s, '1998-07-22')",
                (uid, ta_lang_id)
            )


# ---------------------------------------------------------------------------
# STEP 5: RESTORE THEATRES
# ---------------------------------------------------------------------------

def restore_theatres(cur):
    print("  -> Restoring 25 theatres (5 per city)...")
    cur.executemany(
        "INSERT INTO THEATRE (theatre_id, city_id, theatre_code, theatre_name, address_line_1, "
        "address_line_2, postal_code, latitude, longitude, status) "
        "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)",
        THEATRES
    )

    # Apply deterministic base_price_offset per V6
    cur.execute("UPDATE THEATRE SET base_price_offset = -10.00 WHERE MOD(theatre_id, 5) = 1")
    cur.execute("UPDATE THEATRE SET base_price_offset =  0.00  WHERE MOD(theatre_id, 5) = 2")
    cur.execute("UPDATE THEATRE SET base_price_offset =  10.00 WHERE MOD(theatre_id, 5) = 3")
    cur.execute("UPDATE THEATRE SET base_price_offset =  20.00 WHERE MOD(theatre_id, 5) = 4")
    cur.execute("UPDATE THEATRE SET base_price_offset =  30.00 WHERE MOD(theatre_id, 5) = 0")

    # Update theatre_tier, opening_time, closing_time per Part 1A specification
    cur.execute("""
        UPDATE THEATRE SET theatre_tier = 'FLAGSHIP', opening_time = '08:30:00', closing_time = '01:30:00'
        WHERE theatre_code IN ('DEMO_BLR_CINEPLEX', 'DEMO_BOM_CINEPLEX', 'DEMO_CHN_CINEPLEX',
                               'DEMO_DEL_CINEPLEX', 'DEMO_HYD_CINEPLEX', 'DEMO_HYD_PRASADS')
    """)
    cur.execute("""
        UPDATE THEATRE SET theatre_tier = 'BOUTIQUE', opening_time = '10:00:00', closing_time = '23:00:00'
        WHERE theatre_code IN ('DEMO_BLR_SPI', 'DEMO_BOM_PVR', 'DEMO_CHN_SPI',
                               'DEMO_DEL_INOX', 'DEMO_DEL_PVR')
    """)
    cur.execute("""
        UPDATE THEATRE SET theatre_tier = 'SUBURBAN', opening_time = '09:30:00', closing_time = '23:45:00'
        WHERE theatre_code IN ('DEMO_BLR_AGS', 'DEMO_BLR_INOX', 'DEMO_BLR_PVR',
                               'DEMO_BOM_AGS', 'DEMO_BOM_INOX', 'DEMO_BOM_METRO',
                               'DEMO_CHN_AGS', 'DEMO_CHN_INOX', 'DEMO_CHN_PVR',
                               'DEMO_DEL_AGS', 'DEMO_DEL_METRO', 'DEMO_HYD_AMB',
                               'DEMO_HYD_INOX', 'DEMO_HYD_PVR')
    """)


# ---------------------------------------------------------------------------
# STEP 6: ASSIGN MANAGER
# ---------------------------------------------------------------------------

def assign_manager(cur):
    print("  -> Assigning manager to PVK Cineplex Chennai (DEMO_CHN_CINEPLEX)...")
    cur.execute(
        "INSERT INTO EMPLOYEE_THEATRE (user_id, theatre_id, status) "
        "SELECT u.user_id, t.theatre_id, 'ACTIVE' "
        "FROM USER u, THEATRE t "
        "WHERE u.email = %s AND t.theatre_code = %s",
        (DEMO_MANAGER, MANAGER_THEATRE_CODE)
    )


# ---------------------------------------------------------------------------
# STEP 7: RESTORE SCREENS & CAPABILITIES (Phase 1A: 127 Screens, 3-8 per theatre)
# ---------------------------------------------------------------------------

def restore_screens(cur):
    print("  -> Restoring 127 screens (varied screen counts 3-8 per theatre)...")

    cur.execute("SELECT format_code, presentation_format_id FROM PRESENTATION_FORMAT")
    pf_map = {r["format_code"]: r["presentation_format_id"] for r in cur.fetchall()}
    cur.execute("SELECT format_code, audio_format_id FROM AUDIO_FORMAT")
    af_map = {r["format_code"]: r["audio_format_id"] for r in cur.fetchall()}

    screen_id = 1
    cap_id    = 1
    screens_sql = []
    caps_sql    = []

    for th in THEATRES:
        theatre_id   = th[0]
        theatre_code = th[2]
        arch_names   = THEATRE_SCREENS[theatre_code]
        for idx, arch_name in enumerate(arch_names, 1):
            arch = ARCHETYPES[arch_name]
            scr_code = f"{theatre_code}_S{idx}"
            scr_name = arch["name_template"].format(idx=idx)
            is_premium = 1 if arch_name in {'IMAX_GRAND', 'MOTION_4DX_88', 'DOLBY_GRAND'} else 0
            screens_sql.append((screen_id, theatre_id, scr_code, scr_name, "ACTIVE", is_premium))
            pf_id = pf_map[arch["format_code"]]
            af_id = af_map[arch["audio_code"]]
            caps_sql.append((cap_id, screen_id, pf_id, af_id))
            screen_id += 1
            cap_id    += 1

    cur.executemany(
        "INSERT INTO SCREEN (screen_id, theatre_id, screen_code, screen_name, status, is_premium) "
        "VALUES (%s, %s, %s, %s, %s, %s)",
        screens_sql
    )
    cur.executemany(
        "INSERT INTO SCREEN_CAPABILITY (screen_capability_id, screen_id, presentation_format_id, audio_format_id) "
        "VALUES (%s, %s, %s, %s)",
        caps_sql
    )


# ---------------------------------------------------------------------------
# STEP 8: RESTORE PHYSICAL SEATS (Phase 1A: 16,386 Seats, Varied 64-240)
# ---------------------------------------------------------------------------

def restore_seats(cur):
    print("  -> Restoring 16,386 physical seats across 127 screens (varied 64-240 per screen)...")

    cur.execute("SELECT type_code, seat_type_id FROM SEAT_TYPE")
    st_map = {r["type_code"]: r["seat_type_id"] for r in cur.fetchall()}

    batch = []
    screen_id = 1

    for th in THEATRES:
        theatre_code = th[2]
        arch_names   = THEATRE_SCREENS[theatre_code]
        for arch_name in arch_names:
            arch = ARCHETYPES[arch_name]
            rows = arch["rows"]
            cols = arch["cols"]
            aisles = arch["aisles"]
            row_rules = arch["row_rules"]

            for row_y in range(1, rows + 1):
                row_label = chr(ord("A") + row_y - 1)
                rule = next(r for r in row_rules if r[0] <= row_y <= r[1])
                default_type = rule[2]
                pricing_zone = rule[3]
                accessible_cols = rule[4]

                for col in range(1, cols + 1):
                    seat_num_str = str(col)
                    st_code = "ACCESSIBLE" if col in accessible_cols else default_type
                    st_id = st_map[st_code]
                    aisle_after = 1 if col in aisles else 0

                    batch.append((
                        screen_id, st_id, row_label, seat_num_str,
                        col, row_y, "ACTIVE", pricing_zone, aisle_after
                    ))

                    if len(batch) >= 1000:
                        cur.executemany(
                            "INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, "
                            "position_x, position_y, status, pricing_zone, aisle_after) "
                            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                            batch
                        )
                        batch.clear()

            screen_id += 1

    if batch:
        cur.executemany(
            "INSERT INTO SEAT (screen_id, seat_type_id, row_label, seat_number, "
            "position_x, position_y, status, pricing_zone, aisle_after) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
            batch
        )


# ---------------------------------------------------------------------------
# STEP 9: ENSURE MOVIES (15 Catalogue Movies)
# ---------------------------------------------------------------------------

def ensure_movies(cur):
    cur.execute("SELECT COUNT(*) AS n FROM MOVIE")
    if cur.fetchone()["n"] == 0:
        print("  -> Restoring 15 catalogue movies...")
        cur.execute("""
        INSERT INTO MOVIE (movie_id, certification_id, title, original_title, synopsis, runtime_minutes, release_date, status, poster_url, trailer_url) VALUES
        (1, 2, 'Interstellar', 'Interstellar', 'When Earth becomes uninhabitable, astronaut Cooper leads a desperate mission through a wormhole near Saturn to find humanity a new home.', 169, '2014-11-07', 'AIRING', 'https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIE.jpg', 'https://www.youtube.com/watch?v=zSWdZVtXT7E'),
        (2, 2, 'Avengers: Endgame', 'Avengers: Endgame', 'After Thanos catastrophic Snap erases half of all life, the surviving Avengers assemble for a high-stakes time-heist.', 181, '2019-04-26', 'AIRING', 'https://image.tmdb.org/t/p/w500/or06FN3Dka5tukK1e9sl16pB3iy.jpg', 'https://www.youtube.com/watch?v=TcMBFSGVi1c'),
        (3, 2, 'Oppenheimer', 'Oppenheimer', 'The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.', 180, '2023-07-21', 'AIRING', 'https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 'https://www.youtube.com/watch?v=uYPbbksJxIg'),
        (4, 2, 'Inception', 'Inception', 'Dom Cobb is a skilled thief who steals valuable secrets from deep within the subconscious during the dream state.', 148, '2010-07-16', 'AIRING', 'https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg', 'https://www.youtube.com/watch?v=YoHD9XEInc0'),
        (5, 2, 'Dune: Part Two', 'Dune: Part Two', 'Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators.', 166, '2024-03-01', 'AIRING', 'https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg', 'https://www.youtube.com/watch?v=Way9Dexny3w'),
        (6, 2, 'Vikram', 'Vikram', 'A special investigator is assigned a case of serial killings, leading him to uncover a covert black-ops war.', 173, '2022-06-03', 'AIRING', 'https://image.tmdb.org/t/p/w500/b1xx3Waa0vdBqH5fU1uG5bFzMkW.jpg', 'https://www.youtube.com/watch?v=OKBMCL-frPU'),
        (7, 2, 'Leo', 'Leo: Bloody Sweet', 'A mild-mannered cafe owner in Himachal Pradesh is thrust into danger when violent gangsters mistake him.', 164, '2023-10-19', 'AIRING', 'https://image.tmdb.org/t/p/w500/pD6pqZwNz9Vx2gGB9DTghWF8v9p.jpg', 'https://www.youtube.com/watch?v=Po3jStA673E'),
        (8, 3, 'KGF: Chapter 2', 'Kolar Gold Fields: Chapter 2', 'Rocky reigns over the Kolar Gold Fields, but bloodthirsty rivals threaten his throne.', 168, '2022-04-14', 'AIRING', 'https://image.tmdb.org/t/p/w500/4tFHUDQPAkEd1dIF1vkA49ej7t8.jpg', 'https://www.youtube.com/watch?v=McjLhH-f9oE'),
        (9, 2, 'Kantara', 'Kantara: A Legend', 'A kambala champion clashes with an upright forest officer in a sacred coastal village.', 148, '2022-09-30', 'AIRING', 'https://image.tmdb.org/t/p/w500/dJyPddT47T93Vz83f9Gsm3NqKrn.jpg', 'https://www.youtube.com/watch?v=8mrVmf239GU'),
        (10, 2, 'RRR', 'RRR -- Rise Roar Revolt', 'A fearless warrior and an undercover police officer in 1920s colonial India form an unbreakable bond.', 187, '2022-03-25', 'AIRING', 'https://image.tmdb.org/t/p/w500/nEufeZlyAOLqO6larF8kGaTbGOy.jpg', 'https://www.youtube.com/watch?v=f_vbAtFSEc0'),
        (11, 3, 'Pushpa 2: The Rule', 'Pushpa 2: The Rule', 'Pushpa Raj expands his red sandalwood empire to unprecedented heights.', 200, '2024-12-05', 'AIRING', 'https://image.tmdb.org/t/p/w500/soHLOxQxdEwl0cNRWr9h4Oaiz3Q.jpg', 'https://www.youtube.com/watch?v=mrnVEd-CUU0'),
        (12, 2, 'Jawan', 'Jawan', 'A man driven by a personal vendetta takes on a corrupt arms titan with an army of women.', 169, '2023-09-07', 'AIRING', 'https://image.tmdb.org/t/p/w500/j1WOLodKH1z9NSk5T8eFjF7iXjP.jpg', 'https://www.youtube.com/watch?v=MWOlnZSnXWE'),
        (13, 2, 'Stree 2', 'Stree 2: Sarkate Ka Aatank', 'The town of Chanderi is haunted once more, this time by a headless apparition.', 147, '2024-08-15', 'AIRING', 'https://image.tmdb.org/t/p/w500/mKOBdG5t1607YqA2u9KrnB932Nf.jpg', 'https://www.youtube.com/watch?v=KVnhefZuJhk'),
        (14, 1, '12th Fail', '12th Fail', 'Based on the inspiring real-life journey of IPS officer Manoj Kumar Sharma.', 147, '2023-10-27', 'AIRING', 'https://image.tmdb.org/t/p/w500/z0Gwhw7G4c8v1Zc6pM4L2o4H9xX.jpg', 'https://www.youtube.com/watch?v=we3mQsm7fR8'),
        (15, 2, 'Chhaava', 'Chhaava: The Great Warrior', 'An epic historical chronicle depicting the unyielding valor of Chhatrapati Sambhaji Maharaj.', 160, '2025-02-14', 'AIRING', 'https://image.tmdb.org/t/p/w500/s9Qv1f4Zf6v3k7x5Y7kM3pQ2v1X.jpg', 'https://www.youtube.com/watch?v=7g9p3Y0f8Q4')
        """)
        cur.execute("""
        INSERT INTO MOVIE_GENRE (movie_id, genre_id) VALUES
        (2, 1), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 1), (12, 1), (15, 1),
        (13, 2), (1, 3), (3, 3), (9, 3), (10, 3), (14, 3), (15, 3), (1, 4), (2, 4), (4, 4),
        (5, 4), (3, 5), (4, 5), (6, 5), (7, 5), (8, 5), (11, 5), (12, 5), (13, 8)
        """)
        cur.execute("""
        INSERT INTO MOVIE_LANGUAGE (movie_language_id, movie_id, language_id, language_role) VALUES
        (1, 1, 2, 'ORIGINAL'), (2, 1, 4, 'DUBBED'), (3, 1, 1, 'DUBBED'),
        (4, 2, 2, 'ORIGINAL'), (5, 2, 4, 'DUBBED'), (6, 2, 1, 'DUBBED'), (7, 2, 3, 'DUBBED'),
        (8, 3, 2, 'ORIGINAL'), (9, 3, 4, 'DUBBED'), (10, 4, 2, 'ORIGINAL'), (11, 4, 4, 'DUBBED'),
        (12, 5, 2, 'ORIGINAL'), (13, 5, 4, 'DUBBED'), (14, 6, 1, 'ORIGINAL'), (15, 7, 1, 'ORIGINAL'),
        (16, 8, 6, 'ORIGINAL'), (17, 8, 4, 'DUBBED'), (18, 9, 6, 'ORIGINAL'), (19, 9, 4, 'DUBBED'),
        (20, 10, 3, 'ORIGINAL'), (21, 10, 4, 'DUBBED'), (22, 11, 3, 'ORIGINAL'), (23, 11, 4, 'DUBBED'),
        (24, 12, 4, 'ORIGINAL'), (25, 13, 4, 'ORIGINAL'), (26, 14, 4, 'ORIGINAL'), (27, 15, 4, 'ORIGINAL')
        """)


# ---------------------------------------------------------------------------
# STEP 10: UPDATE DATASET_CONFIG
# ---------------------------------------------------------------------------

def update_dataset_config(cur):
    cur.execute("""
        CREATE TABLE IF NOT EXISTS DATASET_CONFIG (
            config_key   VARCHAR(100) NOT NULL,
            config_value VARCHAR(500) NOT NULL,
            description  VARCHAR(500) NULL DEFAULT NULL,
            updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            CONSTRAINT pk_dataset_config PRIMARY KEY (config_key)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
          COMMENT='PVK Cinemas demo dataset version and seed configuration'
    """)
    now_iso = datetime.datetime.now(datetime.timezone.utc).isoformat()
    entries = [
        ("dataset.version", DATASET_VERSION, "PVK Demo Dataset version identifier"),
        ("dataset.seed", DATASET_SEED, "Deterministic seed value for reproducible master data generation"),
        ("dataset.phase", PHASE, "Current implementation phase (1A = realistic physical cinema ecosystem)"),
        ("dataset.description", "PVK Cinemas Phase 1A Demo: 5 Cities | 25 Theatres | 127 Screens | 16386 Seats (Varied Capacities 64-240) | 3 Demo Users", "Human-readable summary of this dataset"),
        ("dataset.cities", str(EXPECTED_CITIES), "Expected city count for Phase 1A validation"),
        ("dataset.theatres", str(EXPECTED_THEATRES), "Expected theatre count for Phase 1A validation"),
        ("dataset.screens", str(EXPECTED_SCREENS), "Expected screen count for Phase 1A validation"),
        ("dataset.seats_total", str(EXPECTED_SEATS_TOTAL), "Total expected seat count for Phase 1A validation"),
        ("dataset.seats_min", str(EXPECTED_SEATS_MIN), "Minimum expected seat count for Phase 1A validation"),
        ("dataset.theatres_per_city", str(EXPECTED_THEATRES_PER_CITY), "Expected theatres per city for Phase 1A validation"),
        ("dataset.demo_admin", DEMO_ADMIN, "Super Admin demo account"),
        ("dataset.demo_manager", DEMO_MANAGER, "Theatre Manager demo account"),
        ("dataset.demo_customer", DEMO_CUSTOMER, "Customer demo account"),
        ("dataset.manager_theatre_code", MANAGER_THEATRE_CODE, "Theatre code assigned to demo Theatre Manager"),
        ("dataset.last_reset", now_iso, "ISO timestamp of the last successful reset"),
    ]
    cur.executemany("""
        INSERT INTO DATASET_CONFIG (config_key, config_value, description)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description)
    """, entries)
    cur.execute("""
        INSERT INTO DATASET_CONFIG (config_key, config_value, description)
        VALUES ('dataset.reset_count', '1', 'Number of times the demo reset has been executed')
        ON DUPLICATE KEY UPDATE config_value = CAST(config_value AS UNSIGNED) + 1
    """)


# ---------------------------------------------------------------------------
# VALIDATION
# ---------------------------------------------------------------------------

class ValidationResult:
    def __init__(self):
        self.passed = []
        self.failed = []

    def check(self, label, condition, detail=""):
        if condition:
            self.passed.append(label)
            print(f"  [[PASS]] {label}")
        else:
            self.failed.append(f"{label}{(' -- ' + detail) if detail else ''}")
            print(f"  [[FAIL]] {label}{(' -- ' + detail) if detail else ''}")

    @property
    def ok(self):
        return len(self.failed) == 0


def validate(cur) -> ValidationResult:
    vr = ValidationResult()

    # 1. Cities
    cur.execute("SELECT COUNT(*) AS n FROM CITY")
    n = cur.fetchone()["n"]
    vr.check(f"Exactly {EXPECTED_CITIES} cities", n == EXPECTED_CITIES, f"found {n}")

    # 2. Theatres
    cur.execute("SELECT COUNT(*) AS n FROM THEATRE")
    n = cur.fetchone()["n"]
    vr.check(f"Exactly {EXPECTED_THEATRES} theatres", n == EXPECTED_THEATRES, f"found {n}")

    # 3. Theatres per city
    cur.execute(
        "SELECT city_id, COUNT(*) AS n FROM THEATRE GROUP BY city_id HAVING n != %s",
        (EXPECTED_THEATRES_PER_CITY,)
    )
    bad_cities = cur.fetchall()
    vr.check(
        f"Exactly {EXPECTED_THEATRES_PER_CITY} theatres per city",
        len(bad_cities) == 0,
        f"offending city_ids: {[r['city_id'] for r in bad_cities]}"
    )

    # 4. No orphan theatres
    cur.execute(
        "SELECT COUNT(*) AS n FROM THEATRE t LEFT JOIN CITY c ON t.city_id = c.city_id WHERE c.city_id IS NULL"
    )
    n = cur.fetchone()["n"]
    vr.check("No theatres with missing city", n == 0, f"orphan count: {n}")

    # 5. Exactly 127 screens
    cur.execute("SELECT COUNT(*) AS n FROM SCREEN")
    n = cur.fetchone()["n"]
    vr.check(f"Exactly {EXPECTED_SCREENS} screens", n == EXPECTED_SCREENS, f"found {n}")

    # 6. Theatre screen counts intentionally varied (3-8 screens)
    cur.execute("SELECT theatre_id, COUNT(*) AS n FROM SCREEN GROUP BY theatre_id")
    theatre_counts = [r["n"] for r in cur.fetchall()]
    distinct_counts = set(theatre_counts)
    vr.check(
        "Theatre screen counts are intentionally varied (3-8 screens)",
        len(distinct_counts) >= 4 and min(theatre_counts) == 3 and max(theatre_counts) == 8,
        f"distinct screen counts: {sorted(distinct_counts)}"
    )

    # 7. Not every theatre has the same screen count
    vr.check("Not every theatre has the same screen count", len(distinct_counts) > 1)

    # 8. No orphan screens
    cur.execute(
        "SELECT COUNT(*) AS n FROM SCREEN s LEFT JOIN THEATRE t ON s.theatre_id = t.theatre_id WHERE t.theatre_id IS NULL"
    )
    n = cur.fetchone()["n"]
    vr.check("No orphan screens", n == 0, f"orphan count: {n}")

    # 9. Every screen has screen capability
    cur.execute(
        "SELECT COUNT(*) AS n FROM SCREEN s LEFT JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id WHERE sc.screen_capability_id IS NULL"
    )
    n = cur.fetchone()["n"]
    vr.check("Every screen has screen capability", n == 0, f"screens without capability: {n}")

    # 10. Premium formats not universally assigned
    cur.execute(
        "SELECT COUNT(DISTINCT s.screen_id) AS n FROM SCREEN s "
        "JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id "
        "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id "
        "WHERE pf.format_code = 'IMAX'"
    )
    imax_count = cur.fetchone()["n"]
    cur.execute(
        "SELECT COUNT(DISTINCT s.screen_id) AS n FROM SCREEN s "
        "JOIN SCREEN_CAPABILITY sc ON s.screen_id = sc.screen_id "
        "JOIN PRESENTATION_FORMAT pf ON sc.presentation_format_id = pf.presentation_format_id "
        "WHERE pf.format_code = '4DX'"
    )
    fourdx_count = cur.fetchone()["n"]
    vr.check(
        "Premium formats are not assigned universally (IMAX and 4DX in select screens)",
        0 < imax_count < 10 and 0 < fourdx_count < 10,
        f"IMAX screens: {imax_count}, 4DX screens: {fourdx_count}"
    )

    # 11. Every screen has physical seats
    cur.execute(
        "SELECT COUNT(*) AS n FROM SCREEN s LEFT JOIN SEAT se ON s.screen_id = se.screen_id WHERE se.seat_id IS NULL"
    )
    n = cur.fetchone()["n"]
    vr.check("Every screen has physical seats", n == 0, f"screens without seats: {n}")

    # 12. Total seats
    cur.execute("SELECT COUNT(*) AS n FROM SEAT")
    n = cur.fetchone()["n"]
    vr.check(f"Exactly {EXPECTED_SEATS_TOTAL} physical seats (>={EXPECTED_SEATS_MIN})", n == EXPECTED_SEATS_TOTAL, f"found {n}")

    # 13. Screen capacities intentionally varied (64-240 seats)
    cur.execute("SELECT screen_id, COUNT(*) AS n FROM SEAT GROUP BY screen_id")
    screen_caps = [r["n"] for r in cur.fetchall()]
    distinct_caps = set(screen_caps)
    vr.check(
        "Screen capacities are intentionally varied (64-240 seats)",
        len(distinct_caps) >= 5 and min(screen_caps) <= 70 and max(screen_caps) >= 200,
        f"distinct capacities: {sorted(distinct_caps)}"
    )

    # 14. Not every screen has the same capacity
    vr.check("Not every screen has the same capacity", len(distinct_caps) > 1)

    # 15. No duplicate seats within any screen
    cur.execute(
        "SELECT COUNT(*) AS n FROM ("
        "  SELECT screen_id, row_label, seat_number, COUNT(*) AS c FROM SEAT "
        "  GROUP BY screen_id, row_label, seat_number HAVING c > 1"
        ") dupes"
    )
    n = cur.fetchone()["n"]
    vr.check("No duplicate seats within any screen", n == 0, f"duplicate groups: {n}")

    # 16. All seats have valid geometry
    cur.execute("SELECT COUNT(*) AS n FROM SEAT WHERE position_x IS NULL OR position_y IS NULL")
    n = cur.fetchone()["n"]
    vr.check("All seats have valid geometry (position_x, position_y)", n == 0, f"null geometry: {n}")

    # 17. Valid seat numbering
    cur.execute("SELECT COUNT(*) AS n FROM SEAT WHERE seat_number NOT REGEXP '^[0-9]+$'")
    n = cur.fetchone()["n"]
    vr.check("All seats have valid numeric seat_number", n == 0, f"invalid numbers: {n}")

    # 18. Aisles represented correctly
    cur.execute("SELECT COUNT(*) AS n FROM SEAT WHERE aisle_after = 1")
    n = cur.fetchone()["n"]
    vr.check("Aisles are represented correctly (aisle_after=1 seats exist)", n > 0, f"aisle seats: {n}")

    # 19. Valid pricing zones
    cur.execute("SELECT COUNT(*) AS n FROM SEAT WHERE pricing_zone NOT IN ('VALUE', 'STANDARD', 'PREMIUM')")
    n = cur.fetchone()["n"]
    vr.check("All seats have valid pricing_zone", n == 0, f"invalid zones: {n}")

    # 20. Required roles exist
    for role_code in EXPECTED_ROLES:
        cur.execute("SELECT COUNT(*) AS n FROM ROLE WHERE role_code = %s", (role_code,))
        n = cur.fetchone()["n"]
        vr.check(f"Role {role_code} exists", n == 1)

    # 21. Required demo users exist
    for email in EXPECTED_DEMO_USERS:
        cur.execute("SELECT COUNT(*) AS n FROM USER WHERE email = %s", (email,))
        n = cur.fetchone()["n"]
        vr.check(f"Demo user {email} exists", n == 1)

    # 22. No duplicate demo users
    cur.execute(
        "SELECT email, COUNT(*) AS c FROM USER WHERE email IN (%s,%s,%s) GROUP BY email HAVING c > 1",
        (DEMO_ADMIN, DEMO_MANAGER, DEMO_CUSTOMER)
    )
    dups = cur.fetchall()
    vr.check("No duplicate demo users", len(dups) == 0, f"duplicates: {[r['email'] for r in dups]}")

    # 23. Admin has SUPER_ADMIN
    cur.execute(
        "SELECT COUNT(*) AS n FROM USER u JOIN USER_ROLE ur ON u.user_id=ur.user_id "
        "JOIN ROLE r ON ur.role_id=r.role_id "
        "WHERE u.email=%s AND r.role_code='ROLE_SUPER_ADMIN' AND ur.status='ACTIVE'",
        (DEMO_ADMIN,)
    )
    n = cur.fetchone()["n"]
    vr.check(f"{DEMO_ADMIN} has ROLE_SUPER_ADMIN", n == 1)

    # 24. Manager has THEATRE_MANAGER
    cur.execute(
        "SELECT COUNT(*) AS n FROM USER u JOIN USER_ROLE ur ON u.user_id=ur.user_id "
        "JOIN ROLE r ON ur.role_id=r.role_id "
        "WHERE u.email=%s AND r.role_code='ROLE_THEATRE_MANAGER' AND ur.status='ACTIVE'",
        (DEMO_MANAGER,)
    )
    n = cur.fetchone()["n"]
    vr.check(f"{DEMO_MANAGER} has ROLE_THEATRE_MANAGER", n == 1)

    # 25. Customer has CUSTOMER
    cur.execute(
        "SELECT COUNT(*) AS n FROM USER u JOIN USER_ROLE ur ON u.user_id=ur.user_id "
        "JOIN ROLE r ON ur.role_id=r.role_id "
        "WHERE u.email=%s AND r.role_code='ROLE_CUSTOMER' AND ur.status='ACTIVE'",
        (DEMO_CUSTOMER,)
    )
    n = cur.fetchone()["n"]
    vr.check(f"{DEMO_CUSTOMER} has ROLE_CUSTOMER", n == 1)

    # 26. Manager assigned to exactly 1 theatre
    cur.execute(
        "SELECT COUNT(*) AS n FROM USER u JOIN EMPLOYEE_THEATRE et ON u.user_id=et.user_id "
        "WHERE u.email=%s AND et.status='ACTIVE'",
        (DEMO_MANAGER,)
    )
    n = cur.fetchone()["n"]
    vr.check(f"{DEMO_MANAGER} assigned to exactly 1 theatre", n == 1, f"assigned to {n}")

    # 27. Manager assigned to DEMO_CHN_CINEPLEX
    cur.execute(
        "SELECT t.theatre_code FROM USER u "
        "JOIN EMPLOYEE_THEATRE et ON u.user_id=et.user_id "
        "JOIN THEATRE t ON et.theatre_id=t.theatre_id "
        "WHERE u.email=%s AND et.status='ACTIVE'",
        (DEMO_MANAGER,)
    )
    row = cur.fetchone()
    assigned_code = row["theatre_code"] if row else None
    vr.check(
        f"Manager assigned to {MANAGER_THEATRE_CODE}",
        assigned_code == MANAGER_THEATRE_CODE,
        f"actual: {assigned_code}"
    )

    # 28. Flyway schema history intact
    cur.execute(
        "SELECT COUNT(*) AS n FROM information_schema.TABLES "
        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flyway_schema_history'"
    )
    has_flyway = cur.fetchone()["n"] == 1
    vr.check("flyway_schema_history table exists", has_flyway)

    # 29. DATASET_CONFIG table exists
    cur.execute(
        "SELECT COUNT(*) AS n FROM information_schema.TABLES "
        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'DATASET_CONFIG'"
    )
    has_config = cur.fetchone()["n"] == 1
    vr.check("DATASET_CONFIG table exists", has_config)

    # 30. Theatre operating hours and tiers valid
    cur.execute("SELECT COUNT(*) AS n FROM THEATRE WHERE theatre_tier IS NULL OR opening_time IS NULL OR closing_time IS NULL")
    null_theatres = cur.fetchone()["n"]
    cur.execute("SELECT theatre_tier, COUNT(*) AS n FROM THEATRE GROUP BY theatre_tier")
    tier_counts = {r["theatre_tier"]: r["n"] for r in cur.fetchall()}
    vr.check(
        "All 25 theatres have valid operating hours and tiers (6 FLAGSHIP, 5 BOUTIQUE, 14 SUBURBAN)",
        null_theatres == 0 and tier_counts.get("FLAGSHIP") == 6 and tier_counts.get("BOUTIQUE") == 5 and tier_counts.get("SUBURBAN") == 14,
        f"nulls: {null_theatres}, tiers: {tier_counts}"
    )

    # 31. Premium screens populated selectively
    cur.execute("SELECT COUNT(*) AS n FROM SCREEN WHERE is_premium = TRUE")
    prem_count = cur.fetchone()["n"]
    vr.check(
        "Premium screens populated selectively (29 premium screens: 6 IMAX + 4 4DX + 19 Dolby Atmos)",
        prem_count == 29,
        f"premium count: {prem_count}"
    )

    return vr


# ---------------------------------------------------------------------------
# MAIN
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(description="PVK Cinemas Phase 1A Demo Data Reset")
    parser.add_argument("--validate-only", action="store_true",
                        help="Run validation only without modifying data")
    parser.add_argument("--env-file", default=os.path.join(os.path.dirname(__file__), "..", ".env"),
                        help="Path to .env file")
    args = parser.parse_args()

    load_env(args.env_file)

    print()
    print("=" * 60)
    print(f"  PVK CINEMAS -- PHASE 1A DEMO RESET")
    print(f"  Dataset Version : {DATASET_VERSION}")
    print(f"  Seed            : {DATASET_SEED}")
    print(f"  Phase           : {PHASE}")
    print("=" * 60)

    conn = get_connection()

    if args.validate_only:
        print()
        print("[VALIDATE ONLY] Running Phase 1A validation checks...")
        with conn.cursor() as cur:
            vr = validate(cur)
        conn.close()
    else:
        try:
            with conn.cursor() as cur:
                cur.execute("SET FOREIGN_KEY_CHECKS = 0")

                print()
                print("[1/9] Cleaning operational, transactional, and ephemeral data...")
                clean_operational_data(cur)

                print()
                print("[2/9] Cleaning existing master data (screens, seats, users)...")
                clean_master_data(cur)

                cur.execute("SET FOREIGN_KEY_CHECKS = 1")

                print()
                print("[3/9] Restoring 5 canonical cities...")
                restore_cities(cur)

                print()
                print("[4/9] Restoring 3 demo users...")
                restore_demo_users(cur)

                print()
                print("[5/9] Restoring 25 theatres...")
                restore_theatres(cur)

                print()
                print("[6/9] Assigning manager to theatre...")
                assign_manager(cur)

                print()
                print("[7/9] Restoring 127 screens (varied 3-8 per theatre) with capabilities...")
                restore_screens(cur)

                print()
                print("[8/9] Restoring 16,386 physical seats (varied 64-240 per screen)...")
                restore_seats(cur)

                print()
                print("[9/9] Ensuring catalogue reference movies...")
                ensure_movies(cur)

                # Update dataset config
                try:
                    update_dataset_config(cur)
                except Exception as e:
                    print(f"[WARN] Could not update dataset config: {e}")

            conn.commit()
            print()
            print("[[PASS]] Data reset committed.")

        except Exception as e:
            conn.rollback()
            print(f"\n[ERROR] Reset failed, rolled back: {e}")
            conn.close()
            sys.exit(3)

        # Post-reset validation
        print()
        print("[VALIDATION] Running post-reset validation checks...")
        with conn.cursor() as cur:
            vr = validate(cur)
        conn.close()

    print()
    print("=" * 60)
    if vr.ok:
        print(f"  VALIDATION PASSED -- {len(vr.passed)} checks")
        print("=" * 60)
        print()
        sys.exit(0)
    else:
        print(f"  VALIDATION FAILED -- {len(vr.failed)} check(s) failed:")
        for f in vr.failed:
            print(f"    [FAIL] {f}")
        print("=" * 60)
        print()
        sys.exit(1)


if __name__ == "__main__":
    main()

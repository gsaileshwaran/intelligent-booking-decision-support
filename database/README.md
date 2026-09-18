# PVK Cinemas — Database Subsystem

## 1. Overview
This directory contains the authoritative MySQL 8.0 database schema and migration baseline for the **PVK Cinemas** platform. The physical design establishes the relational schema in 3NF and includes end-to-end support for catalogue discovery, multiplex operations, AI show/seat decision support, temporary seat holds, and booking workflows (migrations V1 through V7).

---

## 2. Prerequisites & Environment Specifications

- **Database Engine:** MySQL Server 8.0.x (e.g. Community Server 8.0.45).
- **Default Storage Engine:** `InnoDB` (ACID compliance, row-level locking, foreign key integrity).
- **Character Set & Collation:**
  - `CHARACTER SET utf8mb4`
  - `COLLATE utf8mb4_unicode_ci` (or `utf8mb4_0900_ai_ci`)
- **SQL Mode:** Strict mode enabled (`STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION,ERROR_FOR_DIVISION_BY_ZERO`).
- **Migration Engine:** **Flyway** (Forward-only native SQL migrations).

---

## 3. Directory Layout

```
database/
├── migrations/
│   ├── V1__init_schema.sql                  # Baseline DDL establishing tables, PKs, FKs, UQs, CHECKs, indexes
│   ├── V2__seed_reference_data.sql          # Reference DML: roles, permissions, formats, seat types, cities
│   ├── V3__demo_realistic_seed.sql          # Realistic demo dataset: 5 cities, 25 theatres, 250 screens, 10 movies
│   ├── V4__booking_payment_seat_hold.sql    # Booking, simulated payment, SEAT_HOLD table and permissions
│   ├── V5__demo_dataset_reseed.sql          # Extended realistic show schedule & availability baseline
│   ├── V6__seat_geometry_zones_pricing.sql  # Pricing zones (VALUE, STANDARD, PREMIUM), aisle geometry
│   └── V7__seat_blocking_user_phone.sql     # Manager seat blocking constraints, nullable user phone
└── README.md                                # This document
```

---

## 4. Database Setup & Migration Execution

### Step 1: Create Database Instance
Connect to your local MySQL instance as root or an administrative user:
```sql
CREATE DATABASE IF NOT EXISTS pvk_cinemas_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Step 2: Run Migrations via Flyway CLI
Execute migrations using Flyway against the target database:
```bash
flyway -url="jdbc:mysql://localhost:3306/pvk_cinemas_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
       -user="<db_user>" \
       -password="<db_password>" \
       -locations="filesystem:database/migrations" \
       migrate
```

### Step 3: Verify Migration Status
```bash
flyway -url="jdbc:mysql://localhost:3306/pvk_cinemas_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
       -user="<db_user>" \
       -password="<db_password>" \
       -locations="filesystem:database/migrations" \
       info
```

---

## 5. Security & Privilege Separation

To preserve audit immutability (`BR-008`), separate database credentials are required:

### Migration Administrator (`pvk_migrator`)
Has full DDL and DML privileges to execute Flyway schema upgrades:
```sql
GRANT ALL PRIVILEGES ON pvk_cinemas_db.* TO 'pvk_migrator'@'%';
```

### Runtime Application User (`pvk_app_user`)
Has standard DML on operational tables, but is **RESTRICTED** from modifying or deleting audit trails:
```sql
-- Operational permissions:
GRANT SELECT, INSERT, UPDATE, DELETE ON pvk_cinemas_db.* TO 'pvk_app_user'@'%';

-- Enforce append-only on AUDIT_LOG:
REVOKE UPDATE, DELETE, DROP ON pvk_cinemas_db.AUDIT_LOG FROM 'pvk_app_user'@'%';
```

---

## 6. Verification Queries

After applying migrations, verify the database state in MySQL:

### Verify Table Count (Must be exactly 28 + flyway_schema_history):
```sql
SELECT COUNT(*) AS table_count 
FROM information_schema.tables 
WHERE table_schema = 'pvk_cinemas_db' AND table_name != 'flyway_schema_history';
-- Expected: 28
```

### Verify Reference Seed Data:
```sql
SELECT 'ROLE' AS entity, COUNT(*) AS cnt FROM pvk_cinemas_db.ROLE
UNION ALL SELECT 'PERMISSION', COUNT(*) FROM pvk_cinemas_db.PERMISSION
UNION ALL SELECT 'ROLE_PERMISSION', COUNT(*) FROM pvk_cinemas_db.ROLE_PERMISSION
UNION ALL SELECT 'CITY', COUNT(*) FROM pvk_cinemas_db.CITY
UNION ALL SELECT 'SEAT_TYPE', COUNT(*) FROM pvk_cinemas_db.SEAT_TYPE
UNION ALL SELECT 'GENRE', COUNT(*) FROM pvk_cinemas_db.GENRE
UNION ALL SELECT 'LANGUAGE', COUNT(*) FROM pvk_cinemas_db.LANGUAGE
UNION ALL SELECT 'CERTIFICATION', COUNT(*) FROM pvk_cinemas_db.CERTIFICATION
UNION ALL SELECT 'PRESENTATION_FORMAT', COUNT(*) FROM pvk_cinemas_db.PRESENTATION_FORMAT
UNION ALL SELECT 'AUDIO_FORMAT', COUNT(*) FROM pvk_cinemas_db.AUDIO_FORMAT;
```

---

## 7. Rollback & Recovery Considerations

- **Development:** Drop and recreate the local schema for a clean, deterministic reset:
  ```sql
  DROP DATABASE pvk_cinemas_db;
  CREATE DATABASE pvk_cinemas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```
- **Production / Staging:** Flyway strictly follows a forward-only evolution model. Destructive rollbacks are prohibited; any schema adjustments must be deployed as subsequent forward migrations (`V3__...sql`).

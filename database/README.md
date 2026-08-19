# Database Schema & Seed Management

This directory contains the MySQL relational database definition scripts for the AI Decision Engine for Intelligent Booking platform.

---

## 📁 Directory Structure

* **`schema/schema.sql`**: Full Data Definition Language (DDL) script creating 17 tables, foreign key constraints, indexes, unique constraints, and enum-like checks.
* **`seed/seed.sql`**: Initial development seed script providing system roles, default users, service provider entities, physical venue layouts, sample movies, scheduled shows, and inventory seats.

---

## 🛠 Initialization Instructions

### Using MySQL Command Line Client
```bash
# 1. Login to MySQL Server
mysql -u root -p

# 2. Run DDL Schema Initialization
source database/schema/schema.sql;

# 3. Run Seed Data Population
source database/seed/seed.sql;
```

### Verification Query
```sql
USE `intelligent_booking_db`;
SELECT table_name, table_rows 
FROM information_schema.tables 
WHERE table_schema = 'intelligent_booking_db';
```

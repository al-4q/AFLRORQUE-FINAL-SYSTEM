-- Parking SlotWise — reset all data except admin accounts
--
-- BEFORE YOU RUN THIS:
-- 1. Close the application completely (so the DB file is not locked).
-- 2. Open the correct database file:
--    - Prefer: parking.db in the project folder (same folder you run the app from), OR
--    - If that file does not exist: database/transportation.db
-- 3. Execute this entire script in DB Browser for SQLite, DBeaver, or: sqlite3 parking.db < reset_keep_admin_only.sql
--
-- AFTER:
-- - Restart the app. The system will recreate the special "walkin" user if it is missing.
-- - All parking slots are removed; log in as admin and add slots again to follow the full flow from the beginning.
-- - Every user except role admin is removed (cashiers, customers, walk-in row until the app recreates it).

PRAGMA foreign_keys = OFF;

BEGIN TRANSACTION;

DELETE FROM payments;
DELETE FROM parking_transactions;
DELETE FROM reservations;
DELETE FROM vehicles;
DELETE FROM parking_slots;
DELETE FROM reports;

-- Keep only accounts whose role is admin (case-insensitive). All cashier/user/walk-in rows are removed.
DELETE FROM users WHERE LOWER(TRIM(COALESCE(role, ''))) != 'admin';

-- Optional: reset AUTOINCREMENT counters for emptied tables (cleaner IDs on fresh data)
DELETE FROM sqlite_sequence WHERE name IN (
  'payments',
  'parking_transactions',
  'reservations',
  'vehicles',
  'parking_slots',
  'reports'
);

COMMIT;

PRAGMA foreign_keys = ON;

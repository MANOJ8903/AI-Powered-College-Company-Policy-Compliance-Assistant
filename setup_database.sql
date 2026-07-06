-- ============================================================
-- AI Policy Guardian - Database Setup Script
-- Run this script as root in MySQL Workbench or MySQL CLI
-- ============================================================

-- Step 1: Create the database
CREATE DATABASE IF NOT EXISTS policy_guardian
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Step 2: Create the application user
CREATE USER IF NOT EXISTS 'guardian_user'@'localhost' IDENTIFIED BY 'guardian_pass';

-- Step 3: Grant all privileges on the database to the user
GRANT ALL PRIVILEGES ON policy_guardian.* TO 'guardian_user'@'localhost';

-- Step 4: Apply privilege changes
FLUSH PRIVILEGES;

-- Verify setup
SELECT 'Database setup complete!' AS Status;
SHOW DATABASES LIKE 'policy_guardian';

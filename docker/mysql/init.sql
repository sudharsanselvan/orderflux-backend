-- Runs ONCE when MySQL container starts for the first time
-- Creates database with proper charset for Unicode support

CREATE DATABASE IF NOT EXISTS orderflux_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON orderflux_db.* TO 'orderflux'@'%';
FLUSH PRIVILEGES;
-- ==============================================================================
-- Rentron Phase 1 & 1.1: Database Migration for Booking Engine, Dynamic Pricing & Dispatch
-- MySQL 8.4+ & Flyway Compatible Versioned Migration
-- ==============================================================================

-- 1. Create rental_packages table if not exists
CREATE TABLE IF NOT EXISTS `rental_packages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `vehicle_category` VARCHAR(50) NOT NULL DEFAULT 'ALL',
    `included_hours` DOUBLE NOT NULL DEFAULT 1.0,
    `included_km` DOUBLE NOT NULL DEFAULT 10.0,
    `base_package_fare` DECIMAL(12,2) NOT NULL DEFAULT 349.00,
    `extra_km_price` DECIMAL(12,2) NOT NULL DEFAULT 11.00,
    `extra_hour_price` DECIMAL(12,2) NOT NULL DEFAULT 90.00,
    `booking_fee` DECIMAL(12,2) NOT NULL DEFAULT 18.00,
    `tax_percentage` DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create booking_event_history table if not exists
CREATE TABLE IF NOT EXISTS `booking_event_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `from_status` VARCHAR(50),
    `to_status` VARCHAR(50) NOT NULL,
    `actor_user_id` BIGINT,
    `actor_role` VARCHAR(50),
    `reason` VARCHAR(500),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_beh_booking_id` (`booking_id`),
    INDEX `idx_beh_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Stored Procedure for Safe Idempotent Column Additions in MySQL 8.4 (replaces unsupported ADD COLUMN IF NOT EXISTS)
DROP PROCEDURE IF EXISTS `AddColIfMissing`;
DELIMITER $$
CREATE PROCEDURE `AddColIfMissing`(
    IN in_table_name VARCHAR(64),
    IN in_column_name VARCHAR(64),
    IN in_column_definition TEXT
)
BEGIN
    DECLARE col_count INT;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = in_table_name
      AND COLUMN_NAME = in_column_name;

    IF col_count = 0 THEN
        SET @alter_sql = CONCAT('ALTER TABLE `', in_table_name, '` ADD COLUMN `', in_column_name, '` ', in_column_definition);
        PREPARE stmt FROM @alter_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

-- 4. Apply columns to `bookings` table safely
CALL AddColIfMissing('bookings', 'rental_package_id', 'BIGINT DEFAULT NULL');
CALL AddColIfMissing('bookings', 'scheduled_pickup_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'return_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'pickup_latitude', 'DOUBLE DEFAULT NULL');
CALL AddColIfMissing('bookings', 'pickup_longitude', 'DOUBLE DEFAULT NULL');
CALL AddColIfMissing('bookings', 'drop_latitude', 'DOUBLE DEFAULT NULL');
CALL AddColIfMissing('bookings', 'drop_longitude', 'DOUBLE DEFAULT NULL');
CALL AddColIfMissing('bookings', 'estimated_distance_km', 'DOUBLE DEFAULT 0.0');
CALL AddColIfMissing('bookings', 'estimated_duration_minutes', 'DOUBLE DEFAULT 0.0');
CALL AddColIfMissing('bookings', 'estimated_fare', 'DECIMAL(12,2) DEFAULT 0.00');
CALL AddColIfMissing('bookings', 'final_fare', 'DECIMAL(12,2) DEFAULT 0.00');
CALL AddColIfMissing('bookings', 'fare_breakdown_snapshot', 'LONGTEXT DEFAULT NULL');
CALL AddColIfMissing('bookings', 'ride_start_otp_hash', 'VARCHAR(128) DEFAULT NULL');
CALL AddColIfMissing('bookings', 'otp_verified_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'driver_arrived_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'ride_started_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'ride_completed_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'cancellation_reason', 'VARCHAR(500) DEFAULT NULL');
CALL AddColIfMissing('bookings', 'otp_generated_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'otp_expires_at', 'DATETIME DEFAULT NULL');
CALL AddColIfMissing('bookings', 'otp_attempts', 'INT DEFAULT 0');
CALL AddColIfMissing('bookings', 'otp_max_attempts', 'INT DEFAULT 5');

-- 5. Apply columns to `pricings` table safely
CALL AddColIfMissing('pricings', 'night_allowance', 'DECIMAL(12,2) DEFAULT 300.00');
CALL AddColIfMissing('pricings', 'night_allowance_start_hour', 'INT DEFAULT 23');
CALL AddColIfMissing('pricings', 'night_allowance_end_hour', 'INT DEFAULT 6');
CALL AddColIfMissing('pricings', 'min_outstation_km_one_way', 'DOUBLE DEFAULT 50.0');
CALL AddColIfMissing('pricings', 'min_outstation_km_round_trip', 'DOUBLE DEFAULT 250.0');

-- 6. Apply columns to `booking_settings` table safely
CALL AddColIfMissing('booking_settings', 'dispatch_lead_time_minutes', 'INT DEFAULT 30');

-- 7. Apply columns to `booking_assignments` table safely
CALL AddColIfMissing('booking_assignments', 'rejection_reason', 'VARCHAR(255) DEFAULT NULL');

-- 8. Convert monetary columns to DECIMAL(12,2) precision in MySQL 8.4
ALTER TABLE `bookings` MODIFY COLUMN `estimated_fare` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `final_fare` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `total_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `base_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `distance_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `time_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `waiting_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `driver_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `operator_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `booking_fee` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `discount_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `tax_amount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `bookings` MODIFY COLUMN `driver_tip` DECIMAL(12,2) DEFAULT 0.00;

ALTER TABLE `pricings` MODIFY COLUMN `base_price` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `per_km_price` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `per_minute_price` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `per_hour_price` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `daily_rate` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `extra_hour_rate` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `discount` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `minimum_fare` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `waiting_charge` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `driver_charge` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `operator_charge` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `booking_fee` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `cancellation_fee` DECIMAL(12,2) DEFAULT 0.00;
ALTER TABLE `pricings` MODIFY COLUMN `night_allowance` DECIMAL(12,2) DEFAULT 300.00;

-- Clean up temporary procedure
DROP PROCEDURE IF EXISTS `AddColIfMissing`;

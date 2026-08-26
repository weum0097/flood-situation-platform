USE flood_scenario_deduction;

ALTER TABLE idempotency_record
  ADD COLUMN operation_code VARCHAR(128) NULL AFTER client_id;

UPDATE idempotency_record
SET operation_code = CONCAT('LEGACY:', resource_type)
WHERE operation_code IS NULL;

ALTER TABLE idempotency_record
  MODIFY operation_code VARCHAR(128) NOT NULL,
  DROP INDEX uk_idempotency_client_key,
  ADD UNIQUE KEY uk_idempotency_client_operation_key
    (client_id, operation_code, idempotency_key);

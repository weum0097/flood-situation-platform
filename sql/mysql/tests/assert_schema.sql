USE flood_scenario_deduction;

START TRANSACTION;
INSERT INTO api_audit_log (
  request_id,
  client_id,
  api_key_id,
  http_method,
  request_path,
  response_status,
  duration_ms,
  remote_ip
) VALUES (
  CONCAT('fk-free-test-', UUID()),
  18446744073709551614,
  NULL,
  'GET',
  '/schema-test/orphan-reference',
  200,
  0,
  '127.0.0.1'
);
ROLLBACK;

DROP PROCEDURE IF EXISTS assert_flood_schema;

DELIMITER $$

CREATE PROCEDURE assert_flood_schema()
BEGIN
  DECLARE actual_count INT DEFAULT 0;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.tables
   WHERE table_schema = 'flood_scenario_deduction'
     AND table_type = 'BASE TABLE';
  IF actual_count <> 15 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: expected 15 base tables';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM (
      SELECT 'region' AS table_name
      UNION ALL SELECT 'api_client'
      UNION ALL SELECT 'api_key'
      UNION ALL SELECT 'disaster_event'
      UNION ALL SELECT 'disaster_event_observation'
      UNION ALL SELECT 'situation_rule_set'
      UNION ALL SELECT 'situation_rule'
      UNION ALL SELECT 'region_situation_assessment'
      UNION ALL SELECT 'region_situation_assessment_event'
      UNION ALL SELECT 'material_standard_set'
      UNION ALL SELECT 'material_standard'
      UNION ALL SELECT 'material_demand_calculation'
      UNION ALL SELECT 'material_demand_item'
      UNION ALL SELECT 'idempotency_record'
      UNION ALL SELECT 'api_audit_log'
    ) expected
    LEFT JOIN information_schema.tables actual
      ON actual.table_schema = 'flood_scenario_deduction'
     AND actual.table_name = expected.table_name
     AND actual.table_type = 'BASE TABLE'
   WHERE actual.table_name IS NULL;
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: one or more required tables are missing';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.tables
   WHERE table_schema = 'flood_scenario_deduction'
     AND table_type = 'BASE TABLE'
     AND engine <> 'InnoDB';
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: every table must use InnoDB';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.tables
   WHERE table_schema = 'flood_scenario_deduction'
     AND table_type = 'BASE TABLE'
     AND table_collation <> 'utf8mb4_0900_ai_ci';
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: every table must use utf8mb4_0900_ai_ci';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.key_column_usage
   WHERE table_schema = 'flood_scenario_deduction'
     AND referenced_table_name IS NOT NULL;
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: database foreign keys must be disabled';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.columns
   WHERE table_schema = 'flood_scenario_deduction'
     AND data_type = 'json';
  IF actual_count <> 10 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: expected 10 JSON columns';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM information_schema.columns
   WHERE table_schema = 'flood_scenario_deduction'
     AND table_name = 'material_standard'
     AND column_name = 'region_scope_id'
     AND extra = 'STORED GENERATED'
     AND generation_expression IS NOT NULL;
  IF actual_count <> 1 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: material region scope must be a stored generated column';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM (
      SELECT 'region' AS table_name, 'uk_region_code' AS index_name
      UNION ALL SELECT 'api_client', 'uk_api_client_code'
      UNION ALL SELECT 'api_key', 'uk_api_key_prefix'
      UNION ALL SELECT 'disaster_event', 'uk_disaster_event_public_id'
      UNION ALL SELECT 'disaster_event', 'uk_disaster_event_source_external'
      UNION ALL SELECT 'disaster_event_observation', 'uk_event_observation_public_id'
      UNION ALL SELECT 'disaster_event_observation', 'uk_event_observation_external'
      UNION ALL SELECT 'disaster_event_observation', 'uk_event_observation_time'
      UNION ALL SELECT 'situation_rule_set', 'uk_situation_rule_set_version'
      UNION ALL SELECT 'situation_rule', 'uk_situation_rule_code'
      UNION ALL SELECT 'region_situation_assessment', 'uk_situation_assessment_public_id'
      UNION ALL SELECT 'material_standard_set', 'uk_material_standard_set_version'
      UNION ALL SELECT 'material_standard', 'uk_material_standard'
      UNION ALL SELECT 'material_demand_calculation', 'uk_material_calculation_public_id'
      UNION ALL SELECT 'material_demand_item', 'uk_material_demand_item'
      UNION ALL SELECT 'idempotency_record', 'uk_idempotency_client_key'
      UNION ALL SELECT 'api_audit_log', 'uk_api_audit_request_id'
    ) expected
    LEFT JOIN (
      SELECT DISTINCT table_name, index_name
        FROM information_schema.statistics
       WHERE table_schema = 'flood_scenario_deduction'
         AND non_unique = 0
    ) actual
      ON actual.table_name = expected.table_name
     AND actual.index_name = expected.index_name
   WHERE actual.index_name IS NULL;
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: one or more required unique indexes are missing';
  END IF;

  SELECT COUNT(*)
    INTO actual_count
    FROM (
      SELECT 'chk_region_level' AS constraint_name
      UNION ALL SELECT 'chk_region_status'
      UNION ALL SELECT 'chk_api_client_status'
      UNION ALL SELECT 'chk_api_key_status'
      UNION ALL SELECT 'chk_disaster_event_type'
      UNION ALL SELECT 'chk_disaster_event_status'
      UNION ALL SELECT 'chk_disaster_event_time'
      UNION ALL SELECT 'chk_event_observation_nonnegative'
      UNION ALL SELECT 'chk_situation_rule_set_status'
      UNION ALL SELECT 'chk_situation_rule_direction'
      UNION ALL SELECT 'chk_situation_assessment_level'
      UNION ALL SELECT 'chk_material_standard_nonnegative'
      UNION ALL SELECT 'chk_material_calculation_nonnegative'
      UNION ALL SELECT 'chk_material_item_nonnegative'
    ) expected
    LEFT JOIN information_schema.table_constraints actual
      ON actual.constraint_schema = 'flood_scenario_deduction'
     AND actual.constraint_name = expected.constraint_name
     AND actual.constraint_type = 'CHECK'
   WHERE actual.constraint_name IS NULL;
  IF actual_count <> 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'schema assertion failed: one or more required check constraints are missing';
  END IF;
END$$

DELIMITER ;

CALL assert_flood_schema();
DROP PROCEDURE assert_flood_schema;

SELECT
  VERSION() AS server_version,
  CURRENT_USER() AS authenticated_user,
  DATABASE() AS database_name,
  COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'flood_scenario_deduction'
  AND table_type = 'BASE TABLE';

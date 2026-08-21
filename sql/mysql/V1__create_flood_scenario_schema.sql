SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS flood_scenario_deduction
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE flood_scenario_deduction;

CREATE TABLE IF NOT EXISTS region (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  region_code VARCHAR(64) NOT NULL,
  region_name VARCHAR(128) NOT NULL,
  parent_id BIGINT UNSIGNED NULL,
  region_level VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_region_code (region_code),
  KEY idx_region_name (region_name),
  KEY idx_region_parent (parent_id),
  CONSTRAINT fk_region_parent
    FOREIGN KEY (parent_id) REFERENCES region (id),
  CONSTRAINT chk_region_level
    CHECK (region_level IN ('PROVINCE', 'CITY', 'DISTRICT', 'OTHER')),
  CONSTRAINT chk_region_status
    CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS api_client (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_code VARCHAR(64) NOT NULL,
  client_name VARCHAR(128) NOT NULL,
  organization_name VARCHAR(256) NULL,
  status VARCHAR(16) NOT NULL,
  rate_limit_per_minute INT UNSIGNED NOT NULL,
  allowed_ips JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_api_client_code (client_code),
  CONSTRAINT chk_api_client_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
  CONSTRAINT chk_api_client_rate_limit
    CHECK (rate_limit_per_minute > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS api_key (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT UNSIGNED NOT NULL,
  key_prefix VARCHAR(32) NOT NULL,
  secret_hash BINARY(32) NOT NULL,
  scopes JSON NOT NULL,
  status VARCHAR(16) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  last_used_at DATETIME(3) NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_api_key_prefix (key_prefix),
  KEY idx_api_key_client_status (client_id, status),
  CONSTRAINT fk_api_key_client
    FOREIGN KEY (client_id) REFERENCES api_client (id),
  CONSTRAINT chk_api_key_status
    CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
  CONSTRAINT chk_api_key_dates
    CHECK (last_used_at IS NULL OR last_used_at >= created_at),
  CONSTRAINT chk_api_key_revocation
    CHECK (revoked_at IS NULL OR revoked_at >= created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS disaster_event (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(32) NOT NULL,
  external_event_id VARCHAR(128) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  region_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  event_name VARCHAR(256) NOT NULL,
  start_time DATETIME(3) NOT NULL,
  end_time DATETIME(3) NULL,
  status VARCHAR(16) NOT NULL,
  created_by_client_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
    ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_disaster_event_public_id (public_id),
  UNIQUE KEY uk_disaster_event_source_external
    (source_system, external_event_id),
  KEY idx_disaster_event_region_time (region_id, start_time, end_time),
  KEY idx_disaster_event_region_status_time (region_id, status, start_time),
  KEY idx_disaster_event_created_by (created_by_client_id),
  CONSTRAINT fk_disaster_event_region
    FOREIGN KEY (region_id) REFERENCES region (id),
  CONSTRAINT fk_disaster_event_created_by
    FOREIGN KEY (created_by_client_id) REFERENCES api_client (id),
  CONSTRAINT chk_disaster_event_type
    CHECK (event_type IN (
      'RIVER_FLOOD',
      'URBAN_WATERLOGGING',
      'FLASH_FLOOD',
      'EMBANKMENT_BREACH',
      'DAM_BREAK',
      'OTHER'
    )),
  CONSTRAINT chk_disaster_event_status
    CHECK (status IN ('ONGOING', 'ENDED', 'CANCELLED')),
  CONSTRAINT chk_disaster_event_time
    CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS disaster_event_observation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(32) NOT NULL,
  event_id BIGINT UNSIGNED NOT NULL,
  external_observation_id VARCHAR(128) NULL,
  observed_at DATETIME(3) NOT NULL,
  rainfall_24h_mm DECIMAL(10,2) NULL,
  water_level_over_warning_m DECIMAL(10,3) NULL,
  max_water_depth_m DECIMAL(10,3) NULL,
  affected_area_km2 DECIMAL(14,4) NULL,
  affected_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  trapped_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  evacuated_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  vulnerable_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  injured_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  missing_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  death_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  damaged_households BIGINT UNSIGNED NOT NULL DEFAULT 0,
  collapsed_houses BIGINT UNSIGNED NOT NULL DEFAULT 0,
  road_interruptions INT UNSIGNED NOT NULL DEFAULT 0,
  critical_facilities_affected INT UNSIGNED NOT NULL DEFAULT 0,
  power_outage_households BIGINT UNSIGNED NOT NULL DEFAULT 0,
  source_payload JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_observation_public_id (public_id),
  UNIQUE KEY uk_event_observation_external
    (event_id, external_observation_id),
  UNIQUE KEY uk_event_observation_time (event_id, observed_at),
  CONSTRAINT fk_event_observation_event
    FOREIGN KEY (event_id) REFERENCES disaster_event (id),
  CONSTRAINT chk_event_observation_nonnegative CHECK (
    (rainfall_24h_mm IS NULL OR rainfall_24h_mm >= 0)
    AND (water_level_over_warning_m IS NULL
      OR water_level_over_warning_m >= 0)
    AND (max_water_depth_m IS NULL OR max_water_depth_m >= 0)
    AND (affected_area_km2 IS NULL OR affected_area_km2 >= 0)
    AND affected_population >= 0
    AND trapped_population >= 0
    AND evacuated_population >= 0
    AND vulnerable_population >= 0
    AND injured_population >= 0
    AND missing_population >= 0
    AND death_population >= 0
    AND damaged_households >= 0
    AND collapsed_houses >= 0
    AND road_interruptions >= 0
    AND critical_facilities_affected >= 0
    AND power_outage_households >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS situation_rule_set (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  version VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  aggregation_strategy VARCHAR(32) NOT NULL,
  medium_to_high_count INT UNSIGNED NULL,
  effective_from DATETIME(3) NOT NULL,
  effective_to DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_situation_rule_set_version (version),
  CONSTRAINT chk_situation_rule_set_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
  CONSTRAINT chk_situation_rule_set_strategy
    CHECK (aggregation_strategy = 'HIGHEST_EVENT_LEVEL'),
  CONSTRAINT chk_situation_rule_set_count
    CHECK (medium_to_high_count IS NULL OR medium_to_high_count > 0),
  CONSTRAINT chk_situation_rule_set_time
    CHECK (effective_to IS NULL OR effective_to > effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS situation_rule (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_set_id BIGINT UNSIGNED NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  event_type VARCHAR(32) NULL,
  metric_code VARCHAR(64) NOT NULL,
  comparison_direction VARCHAR(8) NOT NULL,
  medium_threshold DECIMAL(20,4) NULL,
  high_threshold DECIMAL(20,4) NULL,
  priority INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_situation_rule_code (rule_set_id, rule_code),
  CONSTRAINT fk_situation_rule_set
    FOREIGN KEY (rule_set_id) REFERENCES situation_rule_set (id),
  CONSTRAINT chk_situation_rule_event_type CHECK (
    event_type IS NULL OR event_type IN (
      'RIVER_FLOOD',
      'URBAN_WATERLOGGING',
      'FLASH_FLOOD',
      'EMBANKMENT_BREACH',
      'DAM_BREAK',
      'OTHER'
    )
  ),
  CONSTRAINT chk_situation_rule_direction
    CHECK (comparison_direction IN ('GTE', 'LTE')),
  CONSTRAINT chk_situation_rule_thresholds CHECK (
    (medium_threshold IS NULL OR medium_threshold >= 0)
    AND (high_threshold IS NULL OR high_threshold >= 0)
    AND (medium_threshold IS NOT NULL OR high_threshold IS NOT NULL)
  ),
  CONSTRAINT chk_situation_rule_enabled
    CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS region_situation_assessment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(32) NOT NULL,
  region_id BIGINT UNSIGNED NOT NULL,
  assessment_time DATETIME(3) NOT NULL,
  situation_level VARCHAR(16) NOT NULL,
  active_event_count INT UNSIGNED NOT NULL,
  affected_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  trapped_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  evacuated_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  vulnerable_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  rule_set_id BIGINT UNSIGNED NOT NULL,
  rule_version VARCHAR(64) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  input_snapshot JSON NOT NULL,
  warnings JSON NULL,
  created_by_client_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_situation_assessment_public_id (public_id),
  KEY idx_situation_assessment_region_time
    (region_id, assessment_time DESC),
  KEY idx_situation_assessment_region_level_time
    (region_id, situation_level, assessment_time DESC),
  KEY idx_situation_assessment_client_time
    (created_by_client_id, created_at DESC),
  KEY idx_situation_assessment_rule_set (rule_set_id),
  CONSTRAINT fk_situation_assessment_region
    FOREIGN KEY (region_id) REFERENCES region (id),
  CONSTRAINT fk_situation_assessment_rule_set
    FOREIGN KEY (rule_set_id) REFERENCES situation_rule_set (id),
  CONSTRAINT fk_situation_assessment_client
    FOREIGN KEY (created_by_client_id) REFERENCES api_client (id),
  CONSTRAINT chk_situation_assessment_level
    CHECK (situation_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT chk_situation_assessment_source
    CHECK (source_type IN ('DIRECT_IMPORT', 'DATABASE')),
  CONSTRAINT chk_situation_assessment_counts CHECK (
    active_event_count >= 0
    AND affected_population >= 0
    AND trapped_population >= 0
    AND evacuated_population >= 0
    AND vulnerable_population >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS region_situation_assessment_event (
  assessment_id BIGINT UNSIGNED NOT NULL,
  event_id BIGINT UNSIGNED NOT NULL,
  observation_id BIGINT UNSIGNED NOT NULL,
  event_level VARCHAR(16) NOT NULL,
  duration_hours DECIMAL(12,3) NOT NULL,
  matched_rule_codes JSON NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (assessment_id, event_id),
  KEY idx_assessment_event_event (event_id),
  KEY idx_assessment_event_observation (observation_id),
  CONSTRAINT fk_assessment_event_assessment
    FOREIGN KEY (assessment_id) REFERENCES region_situation_assessment (id),
  CONSTRAINT fk_assessment_event_event
    FOREIGN KEY (event_id) REFERENCES disaster_event (id),
  CONSTRAINT fk_assessment_event_observation
    FOREIGN KEY (observation_id) REFERENCES disaster_event_observation (id),
  CONSTRAINT chk_assessment_event_level
    CHECK (event_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT chk_assessment_event_duration
    CHECK (duration_hours >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_standard_set (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  version VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  effective_from DATETIME(3) NOT NULL,
  effective_to DATETIME(3) NULL,
  description VARCHAR(512) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_standard_set_version (version),
  CONSTRAINT chk_material_standard_set_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
  CONSTRAINT chk_material_standard_set_time
    CHECK (effective_to IS NULL OR effective_to > effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_standard (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  standard_set_id BIGINT UNSIGNED NOT NULL,
  region_id BIGINT UNSIGNED NULL,
  region_scope_id BIGINT UNSIGNED
    GENERATED ALWAYS AS (IFNULL(region_id, 0)) STORED,
  situation_level VARCHAR(16) NOT NULL,
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  unit VARCHAR(32) NOT NULL,
  population_basis VARCHAR(32) NOT NULL,
  per_person_per_day DECIMAL(20,6) NULL,
  fixed_base_quantity DECIMAL(20,4) NULL,
  level_factor DECIMAL(10,4) NOT NULL DEFAULT 1,
  reserve_ratio DECIMAL(10,6) NOT NULL DEFAULT 0,
  package_size DECIMAL(20,4) NOT NULL DEFAULT 1,
  minimum_quantity DECIMAL(20,4) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_standard
    (standard_set_id, region_scope_id, situation_level, material_code),
  KEY idx_material_standard_region (region_id),
  CONSTRAINT fk_material_standard_set
    FOREIGN KEY (standard_set_id) REFERENCES material_standard_set (id),
  CONSTRAINT fk_material_standard_region
    FOREIGN KEY (region_id) REFERENCES region (id),
  CONSTRAINT chk_material_standard_level
    CHECK (situation_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT chk_material_standard_basis CHECK (
    population_basis IN (
      'AFFECTED', 'TRAPPED', 'EVACUATED', 'VULNERABLE', 'FIXED'
    )
  ),
  CONSTRAINT chk_material_standard_formula CHECK (
    (population_basis = 'FIXED' AND fixed_base_quantity IS NOT NULL)
    OR (population_basis <> 'FIXED' AND per_person_per_day IS NOT NULL)
  ),
  CONSTRAINT chk_material_standard_nonnegative CHECK (
    (per_person_per_day IS NULL OR per_person_per_day >= 0)
    AND (fixed_base_quantity IS NULL OR fixed_base_quantity >= 0)
    AND level_factor > 0
    AND reserve_ratio >= 0
    AND package_size > 0
    AND minimum_quantity >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_demand_calculation (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  public_id VARCHAR(32) NOT NULL,
  region_id BIGINT UNSIGNED NOT NULL,
  assessment_id BIGINT UNSIGNED NULL,
  source_type VARCHAR(32) NOT NULL,
  situation_level VARCHAR(16) NOT NULL,
  supply_duration_hours DECIMAL(12,3) NOT NULL,
  supply_days INT UNSIGNED NOT NULL,
  affected_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  trapped_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  evacuated_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  vulnerable_population BIGINT UNSIGNED NOT NULL DEFAULT 0,
  standard_set_id BIGINT UNSIGNED NOT NULL,
  standard_version VARCHAR(64) NOT NULL,
  input_snapshot JSON NOT NULL,
  warnings JSON NULL,
  created_by_client_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_calculation_public_id (public_id),
  KEY idx_material_calculation_region_time (region_id, created_at DESC),
  KEY idx_material_calculation_assessment (assessment_id),
  KEY idx_material_calculation_client_time
    (created_by_client_id, created_at DESC),
  KEY idx_material_calculation_standard_set (standard_set_id),
  CONSTRAINT fk_material_calculation_region
    FOREIGN KEY (region_id) REFERENCES region (id),
  CONSTRAINT fk_material_calculation_assessment
    FOREIGN KEY (assessment_id) REFERENCES region_situation_assessment (id),
  CONSTRAINT fk_material_calculation_standard_set
    FOREIGN KEY (standard_set_id) REFERENCES material_standard_set (id),
  CONSTRAINT fk_material_calculation_client
    FOREIGN KEY (created_by_client_id) REFERENCES api_client (id),
  CONSTRAINT chk_material_calculation_source
    CHECK (source_type IN ('DIRECT', 'DATABASE')),
  CONSTRAINT chk_material_calculation_level
    CHECK (situation_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT chk_material_calculation_assessment CHECK (
    (source_type = 'DIRECT' AND assessment_id IS NULL)
    OR (source_type = 'DATABASE' AND assessment_id IS NOT NULL)
  ),
  CONSTRAINT chk_material_calculation_nonnegative CHECK (
    supply_duration_hours > 0
    AND supply_days > 0
    AND affected_population >= 0
    AND trapped_population >= 0
    AND evacuated_population >= 0
    AND vulnerable_population >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS material_demand_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  calculation_id BIGINT UNSIGNED NOT NULL,
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  unit VARCHAR(32) NOT NULL,
  population_basis VARCHAR(32) NOT NULL,
  basis_population BIGINT UNSIGNED NOT NULL,
  gross_demand DECIMAL(20,4) NOT NULL,
  current_inventory DECIMAL(20,4) NOT NULL DEFAULT 0,
  net_demand DECIMAL(20,4) NOT NULL,
  formula_snapshot JSON NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_demand_item (calculation_id, material_code),
  CONSTRAINT fk_material_demand_item_calculation
    FOREIGN KEY (calculation_id) REFERENCES material_demand_calculation (id),
  CONSTRAINT chk_material_item_basis CHECK (
    population_basis IN (
      'AFFECTED', 'TRAPPED', 'EVACUATED', 'VULNERABLE', 'FIXED'
    )
  ),
  CONSTRAINT chk_material_item_nonnegative CHECK (
    basis_population >= 0
    AND gross_demand >= 0
    AND current_inventory >= 0
    AND net_demand >= 0
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_id BIGINT UNSIGNED NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash BINARY(32) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_public_id VARCHAR(32) NULL,
  response_status SMALLINT UNSIGNED NULL,
  response_body JSON NULL,
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotency_client_key (client_id, idempotency_key),
  KEY idx_idempotency_expiry (expires_at),
  CONSTRAINT fk_idempotency_client
    FOREIGN KEY (client_id) REFERENCES api_client (id),
  CONSTRAINT chk_idempotency_resource_type
    CHECK (resource_type IN ('EVENT', 'OBSERVATION', 'ASSESSMENT', 'MATERIAL')),
  CONSTRAINT chk_idempotency_response_status
    CHECK (response_status IS NULL OR response_status BETWEEN 100 AND 599),
  CONSTRAINT chk_idempotency_expiry
    CHECK (expires_at > created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS api_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  request_id VARCHAR(64) NOT NULL,
  client_id BIGINT UNSIGNED NULL,
  api_key_id BIGINT UNSIGNED NULL,
  http_method VARCHAR(8) NOT NULL,
  request_path VARCHAR(512) NOT NULL,
  response_status SMALLINT UNSIGNED NOT NULL,
  error_code VARCHAR(64) NULL,
  duration_ms INT UNSIGNED NOT NULL,
  remote_ip VARCHAR(64) NOT NULL,
  request_hash BINARY(32) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_api_audit_request_id (request_id),
  KEY idx_api_audit_client_time (client_id, created_at DESC),
  KEY idx_api_audit_status_time (response_status, created_at DESC),
  KEY idx_api_audit_error_time (error_code, created_at DESC),
  KEY idx_api_audit_key (api_key_id),
  CONSTRAINT fk_api_audit_client
    FOREIGN KEY (client_id) REFERENCES api_client (id),
  CONSTRAINT fk_api_audit_key
    FOREIGN KEY (api_key_id) REFERENCES api_key (id),
  CONSTRAINT chk_api_audit_http_method
    CHECK (http_method IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE')),
  CONSTRAINT chk_api_audit_response_status
    CHECK (response_status BETWEEN 100 AND 599),
  CONSTRAINT chk_api_audit_duration
    CHECK (duration_ms >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

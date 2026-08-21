USE flood_scenario_deduction;

INSERT INTO region (region_code, region_name, parent_id, region_level, status)
VALUES ('320000', '江苏省', NULL, 'PROVINCE', 'ACTIVE')
ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), status = 'ACTIVE';

INSERT INTO region (region_code, region_name, parent_id, region_level, status)
SELECT '320100', '南京市', id, 'CITY', 'ACTIVE' FROM region WHERE region_code = '320000'
ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), parent_id = VALUES(parent_id), status = 'ACTIVE';

INSERT INTO region (region_code, region_name, parent_id, region_level, status)
SELECT '320111', '浦口区', id, 'DISTRICT', 'ACTIVE' FROM region WHERE region_code = '320100'
ON DUPLICATE KEY UPDATE region_name = VALUES(region_name), parent_id = VALUES(parent_id), status = 'ACTIVE';

INSERT INTO situation_rule_set
  (version, status, aggregation_strategy, medium_to_high_count, effective_from, effective_to)
VALUES ('flood-situation-v1.0', 'ACTIVE', 'HIGHEST_EVENT_LEVEL', 3,
        '2020-01-01 00:00:00.000', NULL)
ON DUPLICATE KEY UPDATE status = 'ACTIVE', aggregation_strategy = VALUES(aggregation_strategy),
  medium_to_high_count = VALUES(medium_to_high_count), effective_from = VALUES(effective_from),
  effective_to = NULL;

INSERT INTO situation_rule
  (rule_set_id, rule_code, event_type, metric_code, comparison_direction,
   medium_threshold, high_threshold, priority, enabled)
SELECT id, 'MAX_WATER_DEPTH', NULL, 'MAX_WATER_DEPTH_M', 'GTE', 0.5000, 1.0000, 100, 1
FROM situation_rule_set WHERE version = 'flood-situation-v1.0'
ON DUPLICATE KEY UPDATE medium_threshold = VALUES(medium_threshold),
  high_threshold = VALUES(high_threshold), enabled = 1;

INSERT INTO situation_rule
  (rule_set_id, rule_code, event_type, metric_code, comparison_direction,
   medium_threshold, high_threshold, priority, enabled)
SELECT id, 'TRAPPED_POPULATION', NULL, 'TRAPPED_POPULATION', 'GTE', 100.0000, 500.0000, 90, 1
FROM situation_rule_set WHERE version = 'flood-situation-v1.0'
ON DUPLICATE KEY UPDATE medium_threshold = VALUES(medium_threshold),
  high_threshold = VALUES(high_threshold), enabled = 1;

INSERT INTO material_standard_set
  (version, status, effective_from, effective_to, description)
VALUES ('material-standard-v1.0', 'ACTIVE', '2020-01-01 00:00:00.000', NULL,
        '本地开发示例，不代表正式救灾政策')
ON DUPLICATE KEY UPDATE status = 'ACTIVE', effective_from = VALUES(effective_from),
  effective_to = NULL, description = VALUES(description);

INSERT INTO material_standard
  (standard_set_id, region_id, situation_level, material_code, material_name, unit,
   population_basis, per_person_per_day, fixed_base_quantity, level_factor,
   reserve_ratio, package_size, minimum_quantity)
SELECT s.id, NULL, levels.level_code, materials.material_code, materials.material_name,
       materials.unit, materials.population_basis, materials.daily_rate, NULL,
       levels.level_factor, 0.100000, materials.package_size, materials.minimum_quantity
FROM material_standard_set s
JOIN (
  SELECT 'LOW' level_code, 1.0000 level_factor UNION ALL
  SELECT 'MEDIUM', 1.2000 UNION ALL
  SELECT 'HIGH', 1.5000
) levels
JOIN (
  SELECT 'DRINKING_WATER' material_code, '饮用水' material_name, 'L' unit,
         'AFFECTED' population_basis, 3.000000 daily_rate, 12.0000 package_size,
         120.0000 minimum_quantity UNION ALL
  SELECT 'INSTANT_FOOD', '方便食品', 'SERVING', 'AFFECTED', 3.000000, 12.0000, 120.0000 UNION ALL
  SELECT 'TENT', '帐篷', 'UNIT', 'EVACUATED', 0.250000, 1.0000, 10.0000 UNION ALL
  SELECT 'MEDICAL_KIT', '医疗包', 'KIT', 'VULNERABLE', 1.000000, 1.0000, 10.0000
) materials
WHERE s.version = 'material-standard-v1.0'
ON DUPLICATE KEY UPDATE material_name = VALUES(material_name), unit = VALUES(unit),
  population_basis = VALUES(population_basis), per_person_per_day = VALUES(per_person_per_day),
  level_factor = VALUES(level_factor), reserve_ratio = VALUES(reserve_ratio),
  package_size = VALUES(package_size), minimum_quantity = VALUES(minimum_quantity);

INSERT INTO material_standard
  (standard_set_id, region_id, situation_level, material_code, material_name, unit,
   population_basis, per_person_per_day, fixed_base_quantity, level_factor,
   reserve_ratio, package_size, minimum_quantity)
SELECT s.id, r.id, 'HIGH', 'DRINKING_WATER', '饮用水', 'L', 'AFFECTED',
       3.500000, NULL, 1.5000, 0.150000, 12.0000, 120.0000
FROM material_standard_set s
JOIN region r ON r.region_code = '320111'
WHERE s.version = 'material-standard-v1.0'
ON DUPLICATE KEY UPDATE per_person_per_day = VALUES(per_person_per_day),
  level_factor = VALUES(level_factor), reserve_ratio = VALUES(reserve_ratio);

USE flood_scenario_deduction;

DROP PROCEDURE IF EXISTS drop_foreign_key_if_exists;

DELIMITER $$

CREATE PROCEDURE drop_foreign_key_if_exists(
  IN target_table VARCHAR(64),
  IN target_constraint VARCHAR(64)
)
BEGIN
  IF EXISTS (
    SELECT 1
      FROM information_schema.table_constraints
     WHERE constraint_schema = 'flood_scenario_deduction'
       AND table_name = target_table
       AND constraint_name = target_constraint
       AND constraint_type = 'FOREIGN KEY'
  ) THEN
    SET @drop_foreign_key_sql = CONCAT(
      'ALTER TABLE `',
      REPLACE(target_table, '`', '``'),
      '` DROP FOREIGN KEY `',
      REPLACE(target_constraint, '`', '``'),
      '`'
    );
    PREPARE drop_foreign_key_statement FROM @drop_foreign_key_sql;
    EXECUTE drop_foreign_key_statement;
    DEALLOCATE PREPARE drop_foreign_key_statement;
  END IF;
END$$

DELIMITER ;

CALL drop_foreign_key_if_exists('region', 'fk_region_parent');
CALL drop_foreign_key_if_exists('api_key', 'fk_api_key_client');
CALL drop_foreign_key_if_exists('disaster_event', 'fk_disaster_event_region');
CALL drop_foreign_key_if_exists('disaster_event', 'fk_disaster_event_created_by');
CALL drop_foreign_key_if_exists(
  'disaster_event_observation',
  'fk_event_observation_event'
);
CALL drop_foreign_key_if_exists('situation_rule', 'fk_situation_rule_set');
CALL drop_foreign_key_if_exists(
  'region_situation_assessment',
  'fk_situation_assessment_region'
);
CALL drop_foreign_key_if_exists(
  'region_situation_assessment',
  'fk_situation_assessment_rule_set'
);
CALL drop_foreign_key_if_exists(
  'region_situation_assessment',
  'fk_situation_assessment_client'
);
CALL drop_foreign_key_if_exists(
  'region_situation_assessment_event',
  'fk_assessment_event_assessment'
);
CALL drop_foreign_key_if_exists(
  'region_situation_assessment_event',
  'fk_assessment_event_event'
);
CALL drop_foreign_key_if_exists(
  'region_situation_assessment_event',
  'fk_assessment_event_observation'
);
CALL drop_foreign_key_if_exists(
  'material_standard',
  'fk_material_standard_set'
);
CALL drop_foreign_key_if_exists(
  'material_standard',
  'fk_material_standard_region'
);
CALL drop_foreign_key_if_exists(
  'material_demand_calculation',
  'fk_material_calculation_region'
);
CALL drop_foreign_key_if_exists(
  'material_demand_calculation',
  'fk_material_calculation_assessment'
);
CALL drop_foreign_key_if_exists(
  'material_demand_calculation',
  'fk_material_calculation_standard_set'
);
CALL drop_foreign_key_if_exists(
  'material_demand_calculation',
  'fk_material_calculation_client'
);
CALL drop_foreign_key_if_exists(
  'material_demand_item',
  'fk_material_demand_item_calculation'
);
CALL drop_foreign_key_if_exists(
  'idempotency_record',
  'fk_idempotency_client'
);
CALL drop_foreign_key_if_exists('api_audit_log', 'fk_api_audit_client');
CALL drop_foreign_key_if_exists('api_audit_log', 'fk_api_audit_key');

DROP PROCEDURE drop_foreign_key_if_exists;
SET @drop_foreign_key_sql = NULL;

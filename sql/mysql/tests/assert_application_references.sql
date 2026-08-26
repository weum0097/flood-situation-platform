-- Read-only orphan checks for the 22 logical references whose foreign keys are removed by V2.
-- Healthy data returns no detail rows and orphan_reference_count = 0.
USE flood_scenario_deduction;

WITH application_reference_check (relation_name, orphan_count) AS (
SELECT 'region.parent_id -> region.id', COUNT(*) FROM region c
LEFT JOIN region p ON p.id = c.parent_id WHERE c.parent_id IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'api_key.client_id -> api_client.id', COUNT(*) FROM api_key c
LEFT JOIN api_client p ON p.id = c.client_id WHERE p.id IS NULL
UNION ALL SELECT 'disaster_event.region_id -> region.id', COUNT(*) FROM disaster_event c
LEFT JOIN region p ON p.id = c.region_id WHERE p.id IS NULL
UNION ALL SELECT 'disaster_event.created_by_client_id -> api_client.id', COUNT(*) FROM disaster_event c
LEFT JOIN api_client p ON p.id = c.created_by_client_id WHERE p.id IS NULL
UNION ALL SELECT 'disaster_event_observation.event_id -> disaster_event.id', COUNT(*) FROM disaster_event_observation c
LEFT JOIN disaster_event p ON p.id = c.event_id WHERE p.id IS NULL
UNION ALL SELECT 'situation_rule.rule_set_id -> situation_rule_set.id', COUNT(*) FROM situation_rule c
LEFT JOIN situation_rule_set p ON p.id = c.rule_set_id WHERE p.id IS NULL
UNION ALL SELECT 'region_situation_assessment.region_id -> region.id', COUNT(*) FROM region_situation_assessment c
LEFT JOIN region p ON p.id = c.region_id WHERE p.id IS NULL
UNION ALL SELECT 'region_situation_assessment.rule_set_id -> situation_rule_set.id', COUNT(*) FROM region_situation_assessment c
LEFT JOIN situation_rule_set p ON p.id = c.rule_set_id WHERE p.id IS NULL
UNION ALL SELECT 'region_situation_assessment.created_by_client_id -> api_client.id', COUNT(*) FROM region_situation_assessment c
LEFT JOIN api_client p ON p.id = c.created_by_client_id WHERE p.id IS NULL
UNION ALL SELECT 'assessment_event.assessment_id -> assessment.id', COUNT(*) FROM region_situation_assessment_event c
LEFT JOIN region_situation_assessment p ON p.id = c.assessment_id WHERE p.id IS NULL
UNION ALL SELECT 'assessment_event.event_id -> disaster_event.id', COUNT(*) FROM region_situation_assessment_event c
LEFT JOIN disaster_event p ON p.id = c.event_id WHERE p.id IS NULL
UNION ALL SELECT 'assessment_event.observation_id -> observation.id', COUNT(*) FROM region_situation_assessment_event c
LEFT JOIN disaster_event_observation p ON p.id = c.observation_id WHERE p.id IS NULL
UNION ALL SELECT 'material_standard.standard_set_id -> standard_set.id', COUNT(*) FROM material_standard c
LEFT JOIN material_standard_set p ON p.id = c.standard_set_id WHERE p.id IS NULL
UNION ALL SELECT 'material_standard.region_id -> region.id', COUNT(*) FROM material_standard c
LEFT JOIN region p ON p.id = c.region_id WHERE c.region_id IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'material_calculation.region_id -> region.id', COUNT(*) FROM material_demand_calculation c
LEFT JOIN region p ON p.id = c.region_id WHERE p.id IS NULL
UNION ALL SELECT 'material_calculation.assessment_id -> assessment.id', COUNT(*) FROM material_demand_calculation c
LEFT JOIN region_situation_assessment p ON p.id = c.assessment_id WHERE c.assessment_id IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'material_calculation.standard_set_id -> standard_set.id', COUNT(*) FROM material_demand_calculation c
LEFT JOIN material_standard_set p ON p.id = c.standard_set_id WHERE p.id IS NULL
UNION ALL SELECT 'material_calculation.created_by_client_id -> api_client.id', COUNT(*) FROM material_demand_calculation c
LEFT JOIN api_client p ON p.id = c.created_by_client_id WHERE p.id IS NULL
UNION ALL SELECT 'material_demand_item.calculation_id -> calculation.id', COUNT(*) FROM material_demand_item c
LEFT JOIN material_demand_calculation p ON p.id = c.calculation_id WHERE p.id IS NULL
UNION ALL SELECT 'idempotency_record.client_id -> api_client.id', COUNT(*) FROM idempotency_record c
LEFT JOIN api_client p ON p.id = c.client_id WHERE p.id IS NULL
UNION ALL SELECT 'api_audit_log.client_id -> api_client.id', COUNT(*) FROM api_audit_log c
LEFT JOIN api_client p ON p.id = c.client_id WHERE c.client_id IS NOT NULL AND p.id IS NULL
UNION ALL SELECT 'api_audit_log.api_key_id -> api_key.id', COUNT(*) FROM api_audit_log c
LEFT JOIN api_key p ON p.id = c.api_key_id WHERE c.api_key_id IS NOT NULL AND p.id IS NULL
)

SELECT relation_name, orphan_count
FROM application_reference_check
WHERE orphan_count > 0
UNION ALL
SELECT 'TOTAL_ORPHAN_REFERENCES', COALESCE(SUM(orphan_count), 0)
FROM application_reference_check
ORDER BY relation_name;

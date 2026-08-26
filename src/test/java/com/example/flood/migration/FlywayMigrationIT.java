package com.example.flood.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class FlywayMigrationIT extends MySqlIntegrationTestBase {
    @Test
    void reachesV4AndKeepsAllApplicationTablesWithoutForeignKeys() {
        Integer version = jdbc.queryForObject(
            "SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history WHERE success=1",
            Integer.class);
        Integer foreignKeys = jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.referential_constraints
            WHERE constraint_schema = DATABASE()
            """, Integer.class);
        Integer tables = jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_type='BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            """, Integer.class);
        assertThat(version).isEqualTo(4);
        assertThat(foreignKeys).isZero();
        assertThat(tables).isEqualTo(15);
    }

    @Test
    void loadsAll2025ProvinceCityAndDistrictCodesWithCompleteParentLinks() {
        Integer regions = jdbc.queryForObject("SELECT COUNT(*) FROM region", Integer.class);
        Integer provinces = jdbc.queryForObject(
            "SELECT COUNT(*) FROM region WHERE region_level='PROVINCE'", Integer.class);
        Integer cities = jdbc.queryForObject(
            "SELECT COUNT(*) FROM region WHERE region_level='CITY'", Integer.class);
        Integer districts = jdbc.queryForObject(
            "SELECT COUNT(*) FROM region WHERE region_level='DISTRICT'", Integer.class);
        Integer missingParents = jdbc.queryForObject("""
            SELECT COUNT(*) FROM region child
            LEFT JOIN region parent ON parent.id = child.parent_id
            WHERE child.region_level <> 'PROVINCE' AND parent.id IS NULL
            """, Integer.class);

        assertThat(regions).isEqualTo(3214);
        assertThat(provinces).isEqualTo(34);
        assertThat(cities).isEqualTo(333);
        assertThat(districts).isEqualTo(2847);
        assertThat(missingParents).isZero();
        assertThat(regionName("110101")).isEqualTo("东城区");
        assertThat(parentCode("320111")).isEqualTo("320100");
        assertThat(parentCode("469001")).isEqualTo("460000");
        assertThat(regionName("710000")).isEqualTo("台湾省");
    }

    @Test
    void applicationReferenceAuditScriptExecutesOnHealthySchema() {
        new ResourceDatabasePopulator(new ClassPathResource(
            "sql/mysql/tests/assert_application_references.sql"))
            .execute(jdbc.getDataSource());
    }

    private String regionName(String code) {
        return jdbc.queryForObject(
            "SELECT region_name FROM region WHERE region_code = ?", String.class, code);
    }

    private String parentCode(String code) {
        return jdbc.queryForObject("""
            SELECT parent.region_code FROM region child
            JOIN region parent ON parent.id = child.parent_id
            WHERE child.region_code = ?
            """, String.class, code);
    }
}

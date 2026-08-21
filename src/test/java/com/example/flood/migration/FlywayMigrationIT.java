package com.example.flood.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.flood.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;

class FlywayMigrationIT extends MySqlIntegrationTestBase {
    @Test
    void reachesV3AndKeepsAllApplicationTablesWithoutForeignKeys() {
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
        assertThat(version).isEqualTo(3);
        assertThat(foreignKeys).isZero();
        assertThat(tables).isEqualTo(15);
    }
}

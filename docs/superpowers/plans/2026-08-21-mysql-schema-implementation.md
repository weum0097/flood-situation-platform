# MySQL Database Schema Implementation Plan

> **Post-implementation decision:** 用户后续选择由应用层维护全部引用完整性。
> V1 中创建的 22 个外键由 `V2__remove_all_foreign_keys.sql` 幂等删除；
> 下文的外键步骤保留为首次实施记录，不代表当前数据库最终状态。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在本机 MySQL 8.4 实例中创建独立的 `flood_scenario_deduction` 数据库及完整的 15 张 MVP 业务表，并提供可重复执行、可验证的 SQL 与 PowerShell 脚本。

**Architecture:** 使用一个版本化初始化脚本创建数据库、表、约束和索引；使用独立的结构断言 SQL 验证表数量、存储引擎、字符集、外键、唯一键和关键检查约束。物资标准与态势阈值只创建承载表，不预置业务值。

**Tech Stack:** MySQL 8.4.9、InnoDB、`utf8mb4_0900_ai_ci`、PowerShell 7/Windows PowerShell、MySQL CLI

**Spec:** `docs/superpowers/specs/2026-08-21-flood-situation-material-api-design.md`

## Global Constraints

- 只创建和修改 `flood_scenario_deduction`，不得修改现有 `ledger_db`、`mysql`、`performance_schema`、`sys`。
- 管理员密码只通过进程环境变量 `FLOOD_DB_ADMIN_PASSWORD` 传入，不写入仓库或命令输出。
- 所有时间字段按 UTC 语义使用 `DATETIME(3)`；所有业务表显式使用 InnoDB 和 `utf8mb4_0900_ai_ci`。
- 物资标准由业务方后续提供，本次不插入 `material_standard_set` 或 `material_standard` 数据。
- 不插入默认 API Key，不保存任何明文密钥。
- 当前工作树中已有的 `test` 删除状态不属于本任务，不得暂存、恢复或修改。
- 未取得显式授权前不执行 Git commit 或 push。

---

### Task 1: Add a failing schema contract

**Files:**
- Create: `sql/mysql/tests/assert_schema.sql`

- [ ] **Step 1: Write the schema assertions before the migration**

  断言必须检查：目标库存在；以下 15 张表全部存在且没有额外业务表；全部为 InnoDB；默认字符集为 `utf8mb4`；关键索引、外键、JSON 列、生成列和检查约束存在。

  表清单固定为：

  ```text
  region
  api_client
  api_key
  disaster_event
  disaster_event_observation
  situation_rule_set
  situation_rule
  region_situation_assessment
  region_situation_assessment_event
  material_standard_set
  material_standard
  material_demand_calculation
  material_demand_item
  idempotency_record
  api_audit_log
  ```

  SQL 使用临时存储过程和 `SIGNAL SQLSTATE '45000'`，任一断言失败时让 MySQL CLI 返回非零退出码；完成后删除临时过程。

- [ ] **Step 2: Run the assertion against the current server and verify it fails**

  ```powershell
  $env:MYSQL_PWD = $env:FLOOD_DB_ADMIN_PASSWORD
  & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' `
    --host=127.0.0.1 --port=3306 --user=root `
    --execute="source sql/mysql/tests/assert_schema.sql"
  Remove-Item Env:MYSQL_PWD
  ```

  Expected: non-zero exit and an assertion message stating that `flood_scenario_deduction` does not yet exist.

### Task 2: Create the versioned database migration

**Files:**
- Create: `sql/mysql/V1__create_flood_scenario_schema.sql`

- [ ] **Step 1: Add database creation and session settings**

  The file must start with:

  ```sql
  SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;
  SET time_zone = '+00:00';

  CREATE DATABASE IF NOT EXISTS flood_scenario_deduction
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

  USE flood_scenario_deduction;
  ```

- [ ] **Step 2: Create access-control and region tables**

  Create, in dependency order, `region`, `api_client`, and `api_key`. Implement all columns from spec sections 4.2–4.4, including:

  - self-reference `region.parent_id -> region.id`;
  - unique keys on `region.region_code`, `api_client.client_code`, and `api_key.key_prefix`;
  - `api_key.client_id -> api_client.id`;
  - indexes `idx_region_name`, `idx_region_parent`, and `idx_api_key_client_status`;
  - status `CHECK` constraints matching the enumerated values in the spec;
  - JSON validity is provided by native MySQL `JSON` columns.

- [ ] **Step 3: Create event tables**

  Create `disaster_event` followed by `disaster_event_observation`, implementing spec sections 4.5–4.6. Required integrity rules include:

  ```sql
  CHECK (end_time IS NULL OR end_time >= start_time)
  UNIQUE KEY uk_disaster_event_source_external (source_system, external_event_id)
  UNIQUE KEY uk_event_observation_external (event_id, external_observation_id)
  UNIQUE KEY uk_event_observation_time (event_id, observed_at)
  ```

  Add non-negative checks for every measurement, population, household, road, and facility count. Database checks cannot compare observation time to the parent event row; document that cross-row rule as an application transaction invariant.

- [ ] **Step 4: Create situation assessment tables**

  Create `situation_rule_set`, `situation_rule`, `region_situation_assessment`, and `region_situation_assessment_event` in that order, implementing spec sections 4.7–4.10. Include the composite evidence primary key `(assessment_id, event_id)`, rule-code uniqueness, all foreign keys, and the three assessment query indexes.

- [ ] **Step 5: Create material calculation tables**

  Create `material_standard_set`, `material_standard`, `material_demand_calculation`, and `material_demand_item`, implementing spec sections 4.11–4.14. The generated scope column and uniqueness must be exactly:

  ```sql
  region_scope_id BIGINT UNSIGNED
    GENERATED ALWAYS AS (IFNULL(region_id, 0)) STORED,
  UNIQUE KEY uk_material_standard
    (standard_set_id, region_scope_id, situation_level, material_code)
  ```

  Add checks for non-negative ratios, quantities, duration, inventory, and demand; `package_size` must be greater than zero.

- [ ] **Step 6: Create idempotency and audit tables**

  Create `idempotency_record` and `api_audit_log`, implementing spec sections 4.15–4.16. Include unique keys `(client_id, idempotency_key)` and `request_id`, all client/key foreign keys, and audit query indexes.

- [ ] **Step 7: Make reruns safe**

  Every table must use `CREATE TABLE IF NOT EXISTS`. The script may not drop or truncate any object. A second run must complete without changing existing data.

### Task 3: Add safe apply and verification wrappers

**Files:**
- Create: `scripts/mysql/apply-schema.ps1`
- Create: `scripts/mysql/verify-schema.ps1`
- Modify: `.gitignore`

- [ ] **Step 1: Write wrapper precondition tests**

  Both scripts must fail with actionable messages when the MySQL client is missing or `FLOOD_DB_ADMIN_PASSWORD` is empty. They must default to `127.0.0.1:3306` and permit overrides through `FLOOD_DB_HOST`, `FLOOD_DB_PORT`, and `FLOOD_DB_ADMIN_USER`.

- [ ] **Step 2: Implement the apply wrapper**

  The apply script must set `MYSQL_PWD` only for the child invocation, execute `V1__create_flood_scenario_schema.sql`, clear `MYSQL_PWD` in a `finally` block, and propagate the MySQL CLI exit code.

- [ ] **Step 3: Implement the verification wrapper**

  The verification script must execute `assert_schema.sql`, clear the temporary password environment variable, and print only a success summary containing server version, current user, database name, and table count.

- [ ] **Step 4: Protect local credential files**

  Add these patterns without changing existing ignore entries:

  ```gitignore
  # Local database credentials
  .env
  .env.*
  !.env.example
  *.mylogin.cnf
  ```

### Task 4: Apply to the local MySQL 8.4 instance

**Files:**
- Execute: `scripts/mysql/apply-schema.ps1`

- [ ] **Step 1: Confirm the protected pre-state**

  Query `SHOW DATABASES` and record that `ledger_db` exists. Query its table count before applying the migration; do not query or print its row data.

- [ ] **Step 2: Set the password only in process memory and apply**

  ```powershell
  $env:FLOOD_DB_ADMIN_PASSWORD = '<provided-at-runtime>'
  & '.\scripts\mysql\apply-schema.ps1'
  Remove-Item Env:FLOOD_DB_ADMIN_PASSWORD
  ```

  The literal password must never be added to a repository file or final response.

- [ ] **Step 3: Run the migration a second time**

  Execute the same wrapper again. Expected: exit code 0, no dropped objects, no data deletion, and no duplicate-object error.

### Task 5: Verify the completed schema

**Files:**
- Execute: `scripts/mysql/verify-schema.ps1`

- [ ] **Step 1: Run all structural assertions**

  Expected summary:

  ```text
  MySQL schema verification passed
  Database: flood_scenario_deduction
  Tables: 15
  Engine: InnoDB
  Charset: utf8mb4
  ```

- [ ] **Step 2: Verify the protected database is unchanged**

  Re-query only the `ledger_db` table count and confirm it matches the pre-state.

- [ ] **Step 3: Inspect repository state**

  ```powershell
  git status --short
  ```

  Confirm that only the new SQL/scripts/docs and the intentional `.gitignore` edit belong to this task, while the pre-existing deleted `test` file remains untouched and unstaged.

- [ ] **Step 4: Commit only if separately authorized**

  If the user explicitly authorizes a commit, stage only the exact task files and commit them. Otherwise, leave all changes uncommitted and report their paths.

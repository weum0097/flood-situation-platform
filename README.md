# Flood Situation Platform

洪水灾害情景态势与物资需求开放 API 的模块化单体 MVP。服务以 MySQL 保存事件、观测、区域态势快照和物资计算结果，对外使用 API Key、Scope、请求幂等和审计日志。

## 范围

当前 MVP 提供八个接口：事件新增、更新、追加观测和区间查询；区域态势计算和区间查询；直接物资计算和基于已保存态势的物资计算。态势等级为 `LOW`、`MEDIUM`、`HIGH`。

当前不包含 GIS、空间叠加、水动力模拟、消息队列、异步任务、JWT/OAuth2 和物资标准管理后台。由于没有 GIS 去重，多个事件的人口统计可能重叠；态势响应会返回 `POSSIBLE_POPULATION_OVERLAP` 警告。`sql/mysql-local` 中的区域、规则和物资标准只用于本地演示，不代表正式救灾政策，生产数据必须由业务方审核提供。

## 运行要求

- JDK 21
- 项目自带 Maven Wrapper
- MySQL 8.0/8.4（本地运行）
- Docker Desktop（运行 Testcontainers 集成测试）

数据库名固定为 `flood_scenario_deduction`，以便与 Flyway 脚本中的 schema 保持一致。复制 [.env.example](.env.example) 中的变量名到自己的环境变量配置。不要提交真实数据库密码、API Key 或 Pepper。API Key 格式为 `flood_live_<8至32位前缀>.<至少32位密文>`。

所有写入数据库的时间最多支持毫秒精度；物资保障时长最多保留 3 位小数，库存数量最多保留 4 位小数。超过精度的请求会返回校验错误，不会静默舍入。

本地 profile 会执行 Flyway V1–V3，并额外加载 `sql/mysql-local/R__local_reference_data.sql`：V1 创建 15 张业务表，V2 删除全部 22 个外键，V3 将幂等键唯一范围调整为“客户端 + 操作 + Key”。已有生产库应按正常 Flyway 顺序迁移，不要启用本地示例数据。

```powershell
$env:FLOOD_DB_USERNAME = "root"
$env:FLOOD_DB_PASSWORD = "your-local-password"
$env:FLOOD_API_KEY_PEPPER = "your-long-random-pepper"
$env:FLOOD_BOOTSTRAP_ENABLED = "true"
$env:FLOOD_BOOTSTRAP_CLIENT_CODE = "local-client"
$env:FLOOD_BOOTSTRAP_API_KEY = "flood_live_localdev1.0123456789abcdefghijklmnopqrstuvwxyz"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

启动后访问：

- 健康检查：`http://localhost:18080/actuator/health`
- Swagger UI：`http://localhost:18080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:18080/v3/api-docs`

## 调用示例

以下命令适用于 Bash/Git Bash。写接口必须同时携带 `X-API-Key` 与 `Idempotency-Key`；查询接口只需 API Key。将返回的 `eventId` 保存到 `EVENT_ID`。

```bash
BASE_URL=http://localhost:18080
API_KEY='flood_live_localdev1.0123456789abcdefghijklmnopqrstuvwxyz'
EVENT_ID='EVT_replace_with_created_id'
```

1. 新增事件

```bash
curl -i -X POST "$BASE_URL/openapi/v1/disaster-events" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-event-create' -H 'Content-Type: application/json' -d '{"externalEventId":"EXT-DEMO-001","sourceSystem":"demo","region":{"regionId":"320111"},"eventType":"RIVER_FLOOD","eventName":"示例洪水","startTime":"2026-08-21T00:00:00Z","status":"ONGOING","initialObservation":{"externalObservationId":"OBS-DEMO-001","observedAt":"2026-08-21T01:00:00Z","hazard":{"rainfall24hMm":180,"maxWaterDepthM":1.2},"impact":{"affectedPopulation":1000,"trappedPopulation":50,"evacuatedPopulation":200,"vulnerablePopulation":80}}}'
```

2. 更新事件

```bash
curl -i -X PUT "$BASE_URL/openapi/v1/disaster-events/$EVENT_ID" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-event-update' -H 'Content-Type: application/json' -d '{"eventType":"RIVER_FLOOD","eventName":"示例洪水（更新）","startTime":"2026-08-21T00:00:00Z","status":"ONGOING"}'
```

3. 追加观测

```bash
curl -i -X POST "$BASE_URL/openapi/v1/disaster-events/$EVENT_ID/observations" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-observation' -H 'Content-Type: application/json' -d '{"externalObservationId":"OBS-DEMO-002","observedAt":"2026-08-21T02:00:00Z","hazard":{"rainfall24hMm":205,"maxWaterDepthM":1.5},"impact":{"affectedPopulation":1500,"trappedPopulation":100,"evacuatedPopulation":300,"vulnerablePopulation":120}}'
```

4. 按区域和时间查询事件

```bash
curl -G "$BASE_URL/openapi/v1/disaster-events" -H "X-API-Key: $API_KEY" --data-urlencode 'regionId=320111' --data-urlencode 'startTime=2026-08-21T00:00:00Z' --data-urlencode 'endTime=2026-08-22T00:00:00Z'
```

5. 导入事件并计算区域态势

```bash
curl -i -X POST "$BASE_URL/openapi/v1/region-situation-assessments" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-assessment' -H 'Content-Type: application/json' -d '{"region":{"regionId":"320111"},"assessmentTime":"2026-08-21T04:00:00Z","events":[{"externalEventId":"EXT-ASSESS-001","sourceSystem":"demo-assess","eventType":"RIVER_FLOOD","eventName":"态势示例事件","startTime":"2026-08-21T00:00:00Z","status":"ONGOING","observation":{"externalObservationId":"OBS-ASSESS-001","observedAt":"2026-08-21T04:00:00Z","hazard":{"maxWaterDepthM":1.5},"impact":{"affectedPopulation":1500,"trappedPopulation":100,"evacuatedPopulation":300,"vulnerablePopulation":120}}}]}'
```

6. 按区域和时间查询已保存态势

```bash
curl -G "$BASE_URL/openapi/v1/region-situation-assessments" -H "X-API-Key: $API_KEY" --data-urlencode 'regionId=320111' --data-urlencode 'startTime=2026-08-21T00:00:00Z' --data-urlencode 'endTime=2026-08-22T00:00:00Z'
```

7. 直接计算物资需求

```bash
curl -i -X POST "$BASE_URL/openapi/v1/material-demand-calculations" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-material-direct' -H 'Content-Type: application/json' -d '{"region":{"regionId":"320111"},"situationLevel":"HIGH","supplyDurationHours":24,"reserveRatio":0.1,"population":{"affectedPopulation":1500,"trappedPopulation":100,"evacuatedPopulation":300,"vulnerablePopulation":120},"currentInventory":[]}'
```

8. 使用区间内最新的已保存态势计算物资需求

```bash
curl -i -X POST "$BASE_URL/openapi/v1/material-demand-calculations/from-region-data" -H "X-API-Key: $API_KEY" -H 'Idempotency-Key: demo-material-region' -H 'Content-Type: application/json' -d '{"region":{"regionId":"320111"},"assessmentTimeRange":{"startTime":"2026-08-21T00:00:00Z","endTime":"2026-08-22T00:00:00Z"},"supplyDurationHours":24,"currentInventory":[]}'
```

同一客户端、同一操作、同一 `Idempotency-Key` 与相同请求体会返回已保存结果；Key 相同但请求不同会返回 `409 IDEMPOTENCY_CONFLICT`。每个响应都会带 `X-Request-Id`，也可由调用方传入该头用于链路追踪。

## 验证与运维

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd -Pintegration verify
powershell -ExecutionPolicy Bypass -File scripts/mysql/tests/test-wrapper-preconditions.ps1
mysql --host=127.0.0.1 --port=3306 --user=root --password flood_scenario_deduction --execute="source sql/mysql/tests/assert_application_references.sql"
```

集成测试会通过 Docker 启动 MySQL 8.4，不会使用或清空本机数据库。引用完整性脚本只读检查 V2 移除外键后的 22 条逻辑引用；健康数据最后应输出 `orphan_reference_count = 0`。

生产环境至少应设置 `FLOOD_DB_USERNAME`、`FLOOD_DB_PASSWORD` 和高强度 `FLOOD_API_KEY_PEPPER`，关闭 Bootstrap（默认关闭），并由受控流程写入客户与 API Key。日志和审计表只保存 Key 对应的内部编号，不保存完整 API Key。

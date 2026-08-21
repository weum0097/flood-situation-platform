# 洪水情景态势与物资需求 API 实现设计

日期：2026-08-21
状态：已完成交互评审，等待文档复核
基础规格：[2026-08-21-flood-situation-material-api-design.md](./2026-08-21-flood-situation-material-api-design.md)

## 1. 目的与优先级

本文把已确认的接口和数据库规格落实为可执行的 Spring Boot 工程设计，用于指导后续逐文件实施计划和编码。

基础规格负责领域字段、请求响应示例、规则公式和错误码；本文负责工程结构、调用链、事务、安全、测试以及数据库兼容处理。若两份文档存在冲突，以本次交互确认并写入本文的内容为准，主要差异如下：

1. 灾害事件创建与更新分为两个接口，开放接口总数由 7 个变为 8 个。
2. 幂等键按“客户端 + API 操作 + 幂等键”隔离，需要新增兼容迁移。
3. 技术基线确定为 Java 21、Spring Boot 3.x、Maven 和 MyBatis-Plus。
4. 采用单 Maven 模块、按业务域分包的模块化单体。

## 2. 实现范围

本次交付：

- 可启动的 Java 21 / Spring Boot / Maven 应用；
- 8 个 `/openapi/v1` 开放接口；
- API Key 认证、scope 授权、单实例限流、请求追踪和审计；
- 写操作幂等控制；
- 事件、观测、态势评估和物资计算的持久化；
- 规则驱动的三级态势计算和版本化物资标准计算；
- Flyway 迁移、local/test 示例数据；
- OpenAPI 文档、单元测试和基于真实 MySQL 的集成测试；
- 本地启动与接口调用说明。

本次不实现：

- GIS、PostGIS、洪水范围空间去重和水动力模拟；
- JWT、OAuth2、用户登录及 API Key 管理端；
- 区域、规则集和物资标准管理端；
- 消息队列、异步计算和分布式限流；
- 自动气象、水文或其他第三方数据接入。

区域、规则集和物资标准由 SQL 或运维数据脚本维护。生产环境不自动写入示例业务数据。

## 3. 工程架构

### 3.1 技术基线

- Java 21；
- Spring Boot 3.x；
- Maven Wrapper；
- Spring Web、Bean Validation、Spring Security；
- MyBatis-Plus、MySQL Driver、Flyway；
- Actuator、springdoc-openapi；
- JUnit 5、AssertJ、Testcontainers MySQL。

实施计划生成时再依据 Spring 官方发布信息固定 Spring Boot 3.x 的具体稳定版本，并由 Maven 依赖管理统一所有 Spring 组件版本。

### 3.2 包结构

```text
com.example.flood
├── FloodApplication
├── common
│   ├── api              # 响应、分页、异常映射、请求 ID
│   ├── idempotency      # 幂等协议与持久化
│   ├── persistence      # 公共 MyBatis 配置和类型处理器
│   └── time             # UTC 转换和 Clock
├── security
│   ├── api              # API Key 过滤器和访问拒绝处理
│   ├── application      # 认证、scope、限流、初始化
│   └── infrastructure   # api_client/api_key Mapper
├── region
│   ├── application      # 区域选择与祖先链解析
│   ├── domain
│   └── infrastructure
├── event
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── situation
│   ├── api
│   ├── application
│   ├── domain           # 规则评估和区域聚合
│   └── infrastructure
├── material
│   ├── api
│   ├── application
│   ├── domain           # 标准选择和公式
│   └── infrastructure
└── audit
    ├── application
    └── infrastructure
```

### 3.3 模块边界

- Controller 只处理 HTTP、DTO 校验和状态码，不包含计算公式。
- 应用服务编排事务和跨模块用例。
- 领域层使用普通 Java 对象实现规则，不依赖 Controller 或 Mapper。
- 模块不得直接调用其他业务模块的 Mapper，只能调用对方应用端口。
- `event` 依赖 `region` 的区域解析端口。
- `situation` 依赖 `region`、`event` 和公共幂等端口。
- `material` 依赖 `region`、`situation` 和公共幂等端口。
- `security` 在进入 Controller 前建立只读调用方上下文。
- `audit` 读取调用方上下文和最终响应信息，不参与业务判定。

首版不引入 Spring Modulith 依赖；通过包可见性和 ArchUnit/模块依赖测试约束边界。

## 4. 开放接口

| 序号 | 方法与路径 | 用途 | Scope |
|---|---|---|---|
| 1 | `POST /openapi/v1/disaster-events` | 创建事件及可选初始观测 | `event:write` |
| 2 | `PUT /openapi/v1/disaster-events/{eventId}` | 完整更新事件可变基础字段 | `event:write` |
| 3 | `POST /openapi/v1/disaster-events/{eventId}/observations` | 追加事件观测 | `event:write` |
| 4 | `GET /openapi/v1/disaster-events` | 按区域和时间查询事件 | `event:read` |
| 5 | `POST /openapi/v1/region-situation-assessments` | 导入当前事件并计算区域态势 | `situation:calculate` |
| 6 | `GET /openapi/v1/region-situation-assessments` | 查询已保存区域态势 | `situation:read` |
| 7 | `POST /openapi/v1/material-demand-calculations` | 直接参数计算物资 | `material:calculate` |
| 8 | `POST /openapi/v1/material-demand-calculations/from-region-data` | 基于数据库最新态势计算物资 | `material:calculate` |

所有 6 个变更或计算接口要求 `Idempotency-Key`；两个 GET 接口不要求。所有接口接收或生成 `X-Request-Id`。

### 4.1 创建和更新事件的边界

创建接口在 `(sourceSystem, externalEventId)` 已存在时返回 `409 EVENT_CONFLICT`，不承担更新语义。

更新接口使用 `PUT`，但请求只包含可变基础字段：

- `eventType`；
- `eventName`；
- `startTime`；
- `endTime`；
- `status`。

以下身份字段不可变：系统 `eventId`、`externalEventId`、`sourceSystem`、`region` 和创建方。观测只能追加，不能被 PUT 覆盖。更新不会重算历史态势；评估表中的输入快照和证据仍代表计算时事实。

### 4.2 区域选择

- 支持 `regionId` 或规范化后的 `regionName`。
- 两者都提供时必须指向同一区域，否则返回 `REGION_SELECTOR_MISMATCH`。
- 名称匹配多条时返回 `REGION_NAME_AMBIGUOUS`。
- 区域不存在或未启用时返回 `REGION_NOT_FOUND`。

### 4.3 时间语义

- HTTP 接口只接受带偏移量或时区的 ISO-8601 时间。
- 应用入口转换为 `Instant`，以 UTC 写入 MySQL `DATETIME(3)`。
- 对外响应默认使用请求时区；无法继承时返回 UTC `Z`。
- 时间区间采用 `[startTime, endTime)`，结束时间必须大于开始时间。

## 5. 安全设计

### 5.1 API Key 认证

调用方通过 `X-API-Key` 传入完整密钥。密钥格式包含可索引前缀和随机 secret：

```text
flood_live_<key-prefix>.<random-secret>
```

认证流程：

1. 解析前缀并查询 `api_key`、`api_client`。
2. 校验客户端及密钥状态、有效期和可选 IP 白名单。
3. 使用 `FLOOD_API_KEY_PEPPER` 对完整密钥计算 HMAC-SHA256。
4. 使用常量时间比较数据库中的 `secret_hash`。
5. 校验目标接口所需 scope。
6. 建立只包含 clientId、apiKeyId、clientCode 和 scopes 的请求上下文。

完整密钥不得进入日志、异常、审计表或响应。`last_used_at` 可节流更新，避免每次请求都产生热点写。

### 5.2 首个密钥初始化

启动初始化读取：

- `FLOOD_BOOTSTRAP_CLIENT_CODE`；
- `FLOOD_BOOTSTRAP_API_KEY`；
- `FLOOD_API_KEY_PEPPER`。

初始化以客户端编码和密钥前缀为自然键创建或更新摘要，保证重复启动安全。生产 profile 缺少安全配置时应用拒绝启动。示例配置文件只声明变量名，不提交真实密钥。

### 5.3 Scope 和限流

Scope 固定为：

- `event:write`；
- `event:read`；
- `situation:calculate`；
- `situation:read`；
- `material:calculate`。

MVP 按 `api_client.rate_limit_per_minute` 使用进程内令牌桶限流。该实现只保证单应用实例内准确；未来多实例部署时替换为共享限流存储，不改变 HTTP 契约。

## 6. 幂等和请求追踪

### 6.1 幂等作用域

幂等唯一作用域为：

```text
client_id + operation_code + idempotency_key
```

`operation_code` 由 HTTP 方法和规范化路由模板组成，例如：

```text
POST:/openapi/v1/disaster-events
PUT:/openapi/v1/disaster-events/{eventId}
```

它等价于已确认的“客户端 + HTTP 方法 + 路径 + 幂等键”，但避免把实际路径参数直接写入长联合索引。

### 6.2 请求摘要和重放

请求摘要包含 operation、规范化路径参数、查询参数和规范化 JSON 请求体。JSON 对象键排序、数字按确定格式输出，随后计算 SHA-256。

- 首次请求：占用幂等记录并执行事务。
- 相同摘要重放：返回首次保存的 HTTP 状态码和响应体。
- 不同摘要重用同一键：返回 `409 IDEMPOTENCY_CONFLICT`。
- 并发首次请求：依靠唯一键竞争；失败方读取获胜记录，已完成则重放，仍处理中则短暂轮询后返回可重试冲突。
- 业务事务回滚时不留下成功业务资源；失败是否短期缓存由错误分类决定，校验类错误不占用幂等键，已进入业务写入的确定性失败可以保存。

幂等窗口由配置控制，默认 24 小时。过期清理由定时任务批量执行。

### 6.3 数据库兼容迁移

现有 `idempotency_record` 的唯一键为 `(client_id, idempotency_key)`，无法实现操作级隔离。新增 `V3` 迁移：

1. 增加 `operation_code VARCHAR(128)`；
2. 为既有空值提供一次性兼容值并改为 `NOT NULL`；
3. 删除 `uk_idempotency_client_key`；
4. 新建唯一键 `(client_id, operation_code, idempotency_key)`。

不恢复任何数据库外键；V2 的“逻辑引用由应用维护”决策保持不变。

### 6.4 请求追踪和审计

- 接受合法 `X-Request-Id`，否则生成新 ID。
- 请求 ID 进入响应头、错误响应、计算快照和 `api_audit_log`。
- 审计过滤器在请求完成后记录方法、路径、状态码、错误码、耗时、来源 IP 和请求摘要。
- 审计写入失败只告警，不反向改变已完成的业务响应。

## 7. 业务用例和事务

### 7.1 创建事件

单事务步骤：

1. 获取幂等执行权。
2. 解析并校验区域。
3. 校验事件类型、状态、时间和人口关系。
4. 检查 `(sourceSystem, externalEventId)` 冲突。
5. 写入 `disaster_event`。
6. 若有初始观测，写入 `disaster_event_observation`。
7. 保存幂等响应并提交。

`public_id` 由服务端生成并对外作为 `eventId`，数据库自增 ID 不对外暴露。

### 7.2 更新事件

单事务步骤：

1. 获取幂等执行权。
2. 按公共 ID 查询并锁定事件。
3. 校验新的时间和状态组合。
4. 更新可变字段。
5. 保存幂等响应并提交。

数据库无版本列，MVP 采用行锁串行化并接受后写覆盖；审计日志保留更新调用。若未来需要调用方乐观并发，再增加版本字段和 `If-Match`，不在本期扩展。

### 7.3 追加观测

- 事件必须存在且观测时刻不得早于事件开始时刻。
- `(event_id, external_observation_id)` 或 `(event_id, observed_at)` 命中且内容一致时返回既有资源。
- 唯一键相同但内容不同返回 `409 OBSERVATION_CONFLICT`。
- 观测是事实快照，开放 API 不提供修改和删除。

### 7.4 导入事件并计算态势

`POST /region-situation-assessments` 是批量导入和计算命令，整个流程单事务：

1. 获取幂等执行权并解析目标区域。
2. 校验所有传入事件属于目标区域。
3. 按 `(sourceSystem, externalEventId)` 导入：不存在则创建；存在则校验区域、事件类型和开始时间等稳定字段，并只更新事件名称、状态和结束时间。
4. 幂等写入每条当前观测。
5. 选择评估时刻有效的 ACTIVE 规则集。
6. 对每个事件选取不晚于评估时刻的最新观测并计算事件等级。
7. 聚合区域等级和人口指标。
8. 写入评估主表、事件证据、输入快照和告警。
9. 保存幂等响应并提交。

任何事件、观测、规则或证据写入失败都整体回滚。

### 7.5 查询历史态势

查询只返回已保存评估和证据，不使用当前规则重算。排序固定为 `assessment_time DESC, id DESC`，分页结果稳定。

### 7.6 直接计算物资

应用服务解析区域、人数、等级、供应时长、储备率和库存；按计算时刻选择 ACTIVE 标准集，并按区域继承链选择标准：

1. 当前区域；
2. 最近上级区域，逐级向上；
3. `region_id IS NULL` 的全局标准。

同一物资只使用最具体的一条标准。整个计算头、明细和幂等响应单事务保存。

### 7.7 基于数据库态势计算物资

应用服务按区域选择最新已保存评估，使用评估中的等级和人口快照，结合请求提供的供应时长及库存计算。若没有态势返回 `NO_SITUATION_DATA`，不隐式现场重算态势。

## 8. 规则引擎

### 8.1 事件等级

规则通过 `metric_code` 映射观测字段，支持 `GTE` 和 `LTE`。同一事件类型优先使用类型专属规则，同时应用事件类型为空的通用规则。

- 任一指标命中 HIGH，事件为 HIGH；
- 否则任一指标命中 MEDIUM，事件为 MEDIUM；
- 都未命中则为 LOW。

每个证据记录命中规则编码、观测 ID、事件等级和持续小时数。

### 8.2 区域等级和人口

- 区域等级默认取事件最高等级。
- 没有事件时为 LOW。
- `medium_to_high_count` 配置存在且 MEDIUM 事件数达到阈值时升级为 HIGH。
- 各事件使用评估时刻之前最新观测的人口值求和。
- 因无 GIS 空间去重，多个事件可能重复覆盖同一人口；结果写入 `warnings` 明确说明。

规则集和输入值都写入评估快照，保证历史可复现。

## 9. 物资计算引擎

```text
supplyDays = ceil(supplyDurationHours / 24)

rawPerPerson =
  basisPopulation * perPersonPerDay * supplyDays
  * levelFactor * (1 + reserveRatio)

rawFixed =
  fixedBaseQuantity * levelFactor * (1 + reserveRatio)

grossDemand = ceilToPackage(max(minimumQuantity, raw), packageSize)
netDemand = max(0, grossDemand - currentInventory)
```

`reserveRatio` 优先使用请求显式值；未提供时使用标准值。请求库存中同一 `materialCode` 只能出现一次，未提供按零处理。库存单位必须与标准单位完全一致，否则返回 `UNIT_MISMATCH`；MVP 不做隐式单位换算。

所有运算使用 `BigDecimal`，禁止二进制浮点。包装向上取整后按照标准精度持久化，公式输入和中间值写入 `formula_snapshot`。

## 10. 无外键条件下的数据完整性

V2 移除了全部外键，因此应用服务必须：

- 写入前验证 region、client、event、observation、rule set、assessment 和 standard set 等逻辑引用；
- 聚合写入使用事务，禁止 Controller 分段写表；
- 停用基础数据前检查业务依赖；本期没有删除管理 API；
- 将唯一索引异常转换为稳定业务错误码；
- 提供只读完整性检查 SQL，用于发现孤立记录。

数据库 CHECK、唯一键和普通索引继续保留，应用校验不能替代数据库的基础约束。

## 11. 配置与数据初始化

### 11.1 Flyway

保留仓库现有 `sql/mysql` 作为唯一结构迁移来源，通过 Maven resources 打入 classpath 并配置 Flyway location，避免复制两套迁移。

- `V1` 创建 15 张表和原始约束；
- `V2` 移除全部外键；
- `V3` 调整幂等操作作用域。

### 11.2 Profile

- `local`：本地 MySQL、开发日志、local 示例业务数据；
- `test`：Testcontainers 动态连接、确定性测试数据；
- `prod`：生产安全校验、无示例业务数据、较保守日志。

local/test 示例数据至少包含区域父子链、一个 ACTIVE 态势规则集和一个 ACTIVE 物资标准集。示例数值只用于验证软件行为，不代表正式救灾标准。

## 12. 错误处理

统一错误结构：

```json
{
  "requestId": "req_...",
  "errorCode": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "details": [],
  "timestamp": "2026-08-21T02:30:00Z"
}
```

全局异常处理稳定映射 `400/401/403/404/409/422/429/500`。数据库 SQL、表名、堆栈和完整 API Key 不对外暴露。

## 13. 测试策略

### 13.1 单元测试

- GTE/LTE 规则边界和缺失指标；
- HIGH 优先、MEDIUM 聚合升级和空事件 LOW；
- 评估时刻之前最新观测选择；
- 供应天数和包装规格向上取整；
- 人均型、固定型、最低保障和库存扣减；
- 区域标准逐级继承、全局回退和单位冲突；
- 区域选择、时间区间、状态转换和错误码。

### 13.2 MySQL 集成测试

使用 Testcontainers MySQL 执行真实 Flyway 迁移，覆盖：

- API Key 成功、失效、scope 不足和密钥脱敏；
- 8 个接口主要成功与失败路径；
- 同操作幂等重放、不同请求冲突、不同操作可复用相同键；
- 并发重复请求只产生一个业务资源；
- 导入评估任一步失败整体回滚；
- 更新事件不改变历史评估快照；
- 历史查询不重算；
- 数据库物资计算选择最新保存评估；
- 无外键时应用层引用校验。

若执行环境没有 Docker，单元测试仍可运行，但集成测试不得被声明为通过；交付说明必须明确记录未执行原因。

## 14. 验收门槛

1. `mvn test` 中全部单元测试通过。
2. 有 Docker 时 MySQL Testcontainers 集成测试通过。
3. 应用可连接本地 MySQL，Flyway 从空库完成 V1-V3。
4. 使用初始化 API Key 可以调用 8 个接口，scope 和错误码符合设计。
5. 幂等、事务回滚、历史快照和物资公式满足基础规格。
6. OpenAPI 文档不展示或记录真实密钥。
7. 仓库原有 SQL 验证脚本继续通过。
8. 不修改、暂存或覆盖与本任务无关的工作树变更。

## 15. 后续流程

本文经用户复核后，使用 Superpowers `writing-plans` 生成逐文件实施计划。实施计划明确测试先行顺序、每个提交的验证命令和交付检查点；在计划获准前不开始业务代码实现。

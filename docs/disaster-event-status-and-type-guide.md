# 灾害事件类型与状态字段说明

本文档说明洪水灾害开放 API 中 `eventType`、`status`、`startTime` 和 `endTime` 的现行取值及校验规则，适用于 Swagger、Postman 和其他外部系统调用。

> 文档基于 2026-08-21 当前实现整理。本文描述的是系统已经实现的行为，不代表未来版本不会扩展。

## 1. `eventType`：灾害事件类型

`eventType` 是一个必填的枚举字段，用于标识洪水灾害的具体类型。当前支持以下 6 个值：

| 枚举值 | 中文名称 | 适用场景 |
| --- | --- | --- |
| `RIVER_FLOOD` | 河流洪水 | 河流水位上涨、超过警戒水位、河水漫溢等事件 |
| `URBAN_WATERLOGGING` | 城市内涝 | 城区积水、道路积水、排水系统超负荷等事件 |
| `FLASH_FLOOD` | 山洪 | 山区短时强降雨引发的快速洪水事件 |
| `EMBANKMENT_BREACH` | 堤防决口 | 河堤、围堤或防洪堤发生决口的事件 |
| `DAM_BREAK` | 溃坝 | 水库大坝或其他拦水设施失事导致的洪水事件 |
| `OTHER` | 其他洪水灾害 | 无法归入上述类型，或当前类型尚未明确的事件 |

### 1.1 填写规则

- 必须填写枚举英文值，不能填写中文名称。
- 枚举值区分大小写，应使用全大写形式。
- JSON 中应填写 `RIVER_FLOOD`，不要填写 `RIVER\_FLOOD`。反斜杠只是在部分 Markdown 文本中用于转义下划线，不是字段值的一部分。
- 暂时无法确认具体类型时，可以使用 `OTHER`。
- 传入未定义的值、中文或小写值时，请求会在 JSON 解析阶段失败，通常返回 HTTP `400`。

正确示例：

```json
{
  "eventType": "URBAN_WATERLOGGING"
}
```

错误示例：

```json
{
  "eventType": "城市内涝"
}
```

```json
{
  "eventType": "river_flood"
}
```

### 1.2 与区域情景态势计算的关系

区域情景态势计算会把 `eventType` 传给规则引擎：

- 未限定事件类型的通用规则，可以用于所有事件。
- 限定了事件类型的规则，只会作用于相同 `eventType` 的事件。
- 因此，应尽量填写准确的类型；`OTHER` 虽然合法，但可能无法命中某些类型专属规则。

## 2. `status`：灾害事件状态

当前支持以下 3 个状态：

| 状态值 | 中文含义 | `startTime` | `endTime` | 是否参与当前区域态势计算 |
| --- | --- | --- | --- | --- |
| `ONGOING` | 正在持续 | 必填 | 必须为空 | 是，但还需满足事件和观测时间条件 |
| `ENDED` | 已结束 | 必填 | 必填 | 否 |
| `CANCELLED` | 已取消或作废 | 必填 | 可为空，也可填写 | 否 |

### 2.1 `ONGOING`：正在持续

表示事件已经发生，目前仍在持续。

字段规则：

- `startTime` 必须填写。
- `endTime` 必须省略或填写为 `null`。
- 如果填写了非空 `endTime`，系统返回 HTTP `422`，错误码为 `VALIDATION_ERROR`。

正确示例：

```json
{
  "eventType": "RIVER_FLOOD",
  "eventName": "浦口区河流洪水",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": null,
  "status": "ONGOING"
}
```

错误示例：

```json
{
  "eventType": "RIVER_FLOOD",
  "eventName": "浦口区河流洪水",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": "2026-08-21T12:00:00.000+08:00",
  "status": "ONGOING"
}
```

上例的状态表示事件仍在持续，但又填写了结束时间，两个字段相互矛盾，因此会返回：

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Invalid event time/status combination"
}
```

### 2.2 `ENDED`：已结束

表示事件已经正常结束。

字段规则：

- `startTime` 必须填写。
- `endTime` 必须填写。
- `endTime` 必须大于或等于 `startTime`。
- `endTime` 缺失、为 `null` 或早于 `startTime` 时，返回 HTTP `422`。

正确示例：

```json
{
  "eventType": "URBAN_WATERLOGGING",
  "eventName": "浦口区城市内涝",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": "2026-08-21T14:30:00.000+08:00",
  "status": "ENDED"
}
```

### 2.3 `CANCELLED`：已取消或作废

表示该事件记录被取消，或者经核实后不应继续作为有效灾害事件使用。例如误报、重复上报或来源撤回。

现行代码规则：

- `startTime` 必须填写。
- `endTime` 可以省略或填写为 `null`。
- `endTime` 也可以填写；如果填写，必须大于或等于 `startTime`。
- 该状态不参与当前区域情景态势计算。

未填写结束时间的合法示例：

```json
{
  "eventType": "FLASH_FLOOD",
  "eventName": "山洪误报记录",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": null,
  "status": "CANCELLED"
}
```

## 3. 时间字段通用规则

### 3.1 时间格式

`startTime`、`endTime` 和观测数据的 `observedAt` 均使用带时区偏移的 ISO 8601 时间格式。

推荐使用以下任一种形式：

```text
2026-08-21T09:01:58.775Z
2026-08-21T17:01:58.775+08:00
```

其中：

- `Z` 表示 UTC。
- `+08:00` 表示东八区。
- 系统最多支持毫秒精度，即小数点后最多 3 位。

以下时间包含微秒，不符合当前精度要求：

```text
2026-08-21T09:01:58.775123Z
```

### 3.2 时间先后关系

只要 `endTime` 不为空，都必须满足：

```text
endTime >= startTime
```

事件观测时间必须满足：

```text
observedAt >= startTime
```

## 4. 状态与区域情景态势计算

事件只有同时满足以下条件，才会参与指定时刻的区域情景态势计算：

1. `status` 为 `ONGOING`。
2. `startTime` 早于或等于 `assessmentTime`。
3. 本次使用的观测记录 `observedAt` 早于或等于 `assessmentTime`。

`ENDED` 和 `CANCELLED` 事件可以保留在数据库中并用于历史查询，但不会被计入当前态势等级的聚合计算。

## 5. 创建与更新接口的区别

### 5.1 创建事件

接口：

```http
POST /openapi/v1/disaster-events
```

创建时的主要字段：

| 字段 | 是否必填 | 说明 |
| --- | --- | --- |
| `externalEventId` | 是 | 调用方系统中的事件编号 |
| `sourceSystem` | 是 | 事件来源系统 |
| `region` | 是 | 使用 `regionId` 或 `regionName` 定位区域 |
| `eventType` | 是 | 本文第 1 节中的事件类型 |
| `eventName` | 是 | 事件名称 |
| `startTime` | 是 | 事件开始时间 |
| `endTime` | 视状态而定 | 受 `status` 规则约束 |
| `status` | 是 | 本文第 2 节中的状态 |
| `initialObservation` | 否 | 创建事件时一并写入的首条灾情观测 |

完整创建示例：

```json
{
  "externalEventId": "EXT-FLOOD-20260821-001",
  "sourceSystem": "EMERGENCY_PLATFORM",
  "region": {
    "regionId": "320111"
  },
  "eventType": "RIVER_FLOOD",
  "eventName": "浦口区河流洪水",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": null,
  "status": "ONGOING",
  "initialObservation": {
    "externalObservationId": "OBS-EXT-001",
    "observedAt": "2026-08-21T09:00:00.000+08:00",
    "hazard": {
      "rainfall24hMm": 200,
      "waterLevelOverWarningM": 0.8,
      "maxWaterDepthM": 1.2,
      "affectedAreaKm2": 15.5
    },
    "impact": {
      "affectedPopulation": 30000,
      "trappedPopulation": 2000,
      "evacuatedPopulation": 100,
      "vulnerablePopulation": 500,
      "injuredPopulation": 10,
      "missingPopulation": 0,
      "deathPopulation": 0,
      "damagedHouseholds": 200,
      "collapsedHouses": 5,
      "roadInterruptions": 8,
      "criticalFacilitiesAffected": 2,
      "powerOutageHouseholds": 1000
    }
  }
}
```

### 5.2 更新事件

接口：

```http
PUT /openapi/v1/disaster-events/{eventId}
```

当前更新接口采用完整替换方式，以下字段必须在每次请求中提交：

- `eventType`
- `eventName`
- `startTime`
- `status`
- `endTime` 是否填写由目标状态决定

`eventId` 必须使用创建接口返回的公开事件编号，例如：

```text
EVT_01m0hp1as75c71rfyb11py43ra
```

不能使用数据库自增主键或自定义数字，例如 `1002`；否则会返回 HTTP `404` 和错误码 `EVENT_NOT_FOUND`。

将持续中的事件更新为已结束：

```json
{
  "eventType": "RIVER_FLOOD",
  "eventName": "浦口区河流洪水",
  "startTime": "2026-08-21T08:00:00.000+08:00",
  "endTime": "2026-08-21T18:00:00.000+08:00",
  "status": "ENDED"
}
```

### 5.3 当前状态流转行为

当前版本没有实现严格的状态流转状态机。只要更新后的字段组合满足时间校验，接口允许在 `ONGOING`、`ENDED` 和 `CANCELLED` 之间更新。

例如，从技术实现上，`ENDED` 可以再次更新为 `ONGOING`，但此时必须把 `endTime` 改为 `null`。是否要在后续版本中禁止此类逆向流转，需要由业务规则进一步确定。

## 6. 观测数据与事件状态的现行关系

添加事件观测的接口为：

```http
POST /openapi/v1/disaster-events/{eventId}/observations
```

当前版本已实现以下限制：

- `observedAt` 不能为空。
- `observedAt` 不能早于事件的 `startTime`。
- 时间最多支持毫秒精度。
- 灾害强度和受灾影响数值不能为负数。
- 受困人数、转移人数、脆弱人群数都不能大于受影响人数。

当前版本暂未限制以下行为：

- 仍然可以向 `ENDED` 或 `CANCELLED` 事件追加观测。
- 当事件存在 `endTime` 时，没有校验 `observedAt` 必须小于或等于 `endTime`。

这两项属于现行实现边界。外部调用方应避免向已结束或已取消事件追加新的有效观测。

## 7. 常见错误说明

| HTTP 状态码 | 常见错误码 | 典型原因 |
| --- | --- | --- |
| `400` | `MALFORMED_REQUEST` | JSON 格式错误、枚举值不存在、枚举大小写错误或时间格式无法解析 |
| `404` | `REGION_NOT_FOUND` | `regionId` 或 `regionName` 在有效区域数据中不存在 |
| `404` | `EVENT_NOT_FOUND` | 更新或追加观测时没有使用有效的 `EVT_...` 事件编号 |
| `409` | `EVENT_CONFLICT` | 相同 `sourceSystem` 和 `externalEventId` 已存在，或导入事件的稳定字段冲突 |
| `409` | `IDEMPOTENCY_CONFLICT` | 同一个幂等键被用于不同请求内容 |
| `422` | `VALIDATION_ERROR` | 状态与开始/结束时间组合不合法、时间精度超过毫秒或观测指标不合法 |

## 8. 快速检查表

在 Swagger 或 Postman 提交事件前，建议依次确认：

- `eventType` 是否为 6 个合法枚举值之一。
- `status` 是否为 `ONGOING`、`ENDED` 或 `CANCELLED`。
- `ONGOING` 的 `endTime` 是否为空。
- `ENDED` 的 `endTime` 是否已填写。
- 非空 `endTime` 是否大于或等于 `startTime`。
- 时间是否包含 `Z` 或明确的时区偏移。
- 时间小数部分是否不超过 3 位。
- 更新事件时是否使用接口返回的 `EVT_...` 编号。
- `regionId` 或 `regionName` 是否对应数据库中的有效区域。


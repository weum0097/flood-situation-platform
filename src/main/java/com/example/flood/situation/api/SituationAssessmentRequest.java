package com.example.flood.situation.api;

import com.example.flood.region.application.RegionSelector;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "区域洪水情景态势计算请求；传入事件会同步导入数据库")
public record SituationAssessmentRequest(
    @Schema(description = "待评估的行政区域") @Valid @NotNull RegionSelector region,
    @Schema(description = "态势评估时刻，必须使用北京时间偏移 +08:00，最多毫秒精度",
        example = "2026-08-21T10:00:00.000+08:00") @NotNull OffsetDateTime assessmentTime,
    @Schema(description = "该区域在评估时刻掌握的灾害事件，至少一条")
    @Valid @NotEmpty List<SituationEventImportRequest> events
) {
    public SituationAssessmentRequest {
        events = events == null ? null : List.copyOf(events);
    }
}

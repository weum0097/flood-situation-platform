package com.example.flood.region.application;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "行政区域选择器。regionId 和 regionName 至少填写一个；同时填写时必须指向同一区域")
public record RegionSelector(
    @Schema(description = "行政区域编码，优先使用该字段定位区域", example = "320111")
    String regionId,
    @Schema(description = "行政区域名称；仅使用名称时必须能够唯一匹配", example = "浦口区")
    String regionName
) {
    public RegionSelector {
        regionId = blankToNull(regionId);
        regionName = blankToNull(regionName);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}

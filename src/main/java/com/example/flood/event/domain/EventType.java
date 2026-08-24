package com.example.flood.event.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "灾害事件类型：RIVER_FLOOD 河流洪水；URBAN_WATERLOGGING 城市内涝；"
    + "FLASH_FLOOD 山洪；EMBANKMENT_BREACH 堤防决口；DAM_BREAK 溃坝；OTHER 其他")
public enum EventType { RIVER_FLOOD, URBAN_WATERLOGGING, FLASH_FLOOD, EMBANKMENT_BREACH, DAM_BREAK, OTHER }

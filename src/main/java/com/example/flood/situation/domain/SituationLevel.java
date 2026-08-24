package com.example.flood.situation.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "区域或事件态势等级：LOW 低、MEDIUM 中、HIGH 高")
public enum SituationLevel { LOW, MEDIUM, HIGH }

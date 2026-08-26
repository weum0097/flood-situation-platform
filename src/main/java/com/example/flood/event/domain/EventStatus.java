package com.example.flood.event.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "事件状态：ONGOING 持续中且 endTime 必须为空；"
    + "ENDED 已结束且 endTime 必填；CANCELLED 已取消且 endTime 可空")
public enum EventStatus { ONGOING, ENDED, CANCELLED }

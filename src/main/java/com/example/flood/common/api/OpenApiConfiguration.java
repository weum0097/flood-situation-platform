package com.example.flood.common.api;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {
    @Bean
    OpenAPI floodOpenApi() {
        String scheme = "ApiKeyAuth";
        return new OpenAPI()
            .info(new Info().title("洪水灾害态势与物资需求开放 API")
                .version("v1").description(
                    "面向外部系统的洪水灾害事件、区域情景态势和物资需求计算接口。"
                        + "所有请求和响应时间统一使用北京时间（Asia/Shanghai，UTC+08:00）；"
                        + "请求时间必须显式使用 +08:00 偏移，Z 或其他偏移将返回 422。"))
            .components(new Components().addSecuritySchemes(scheme,
                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER).name("X-API-Key")
                    .description("调用方 API Key。仅填写原始密钥值，不要添加 Bearer 前缀。")))
            .addSecurityItem(new SecurityRequirement().addList(scheme));
    }

    @Bean
    OpenApiCustomizer commonApiContractCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            ModelConverters.getInstance().readAll(ApiErrorResponse.class)
                .forEach(components::addSchemas);
            components.addParameters("XRequestId", new Parameter().in("header")
                .name("X-Request-Id").required(false)
                .description("可选的调用方请求标识，用于链路追踪；省略时由服务端自动生成")
                .example("req-swagger-20260824-001"));
            components.addHeaders("XRequestId", new Header()
                .description("用于链路追踪的请求标识"));

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                operation.addParametersItem(new Parameter()
                    .$ref("#/components/parameters/XRequestId"));
                Map.of(
                    "400", "请求格式、枚举值或时间格式错误",
                    "401", "API Key 缺失或无效",
                    "403", "API Key 不具备所需权限范围",
                    "404", "区域、事件或态势评估记录不存在",
                    "409", "资源标识、数据或幂等键冲突",
                    "422", "请求通过格式校验，但不满足业务规则",
                    "429", "调用频率超过客户端限额",
                    "500", "服务端内部错误"
                ).forEach((status, description) -> operation.getResponses().putIfAbsent(status,
                    errorResponse(description)));
                operation.getResponses().values().forEach(response -> response.addHeaderObject(
                    "X-Request-Id", new Header().$ref("#/components/headers/XRequestId")));
            }));
        };
    }

    private static ApiResponse errorResponse(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType(
            "application/problem+json", new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))));
    }
}

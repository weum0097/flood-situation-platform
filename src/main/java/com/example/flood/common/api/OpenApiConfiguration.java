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
            .info(new Info().title("Flood Situation and Material Demand Open API")
                .version("v1").description("MVP without GIS or hydraulic simulation"))
            .components(new Components().addSecuritySchemes(scheme,
                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER).name("X-API-Key")))
            .addSecurityItem(new SecurityRequirement().addList(scheme));
    }

    @Bean
    OpenApiCustomizer commonApiContractCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            ModelConverters.getInstance().read(ApiErrorResponse.class)
                .forEach(components::addSchemas);
            components.addParameters("XRequestId", new Parameter().in("header")
                .name("X-Request-Id").required(false)
                .description("Optional caller request identifier; generated when omitted"));
            components.addHeaders("XRequestId", new Header()
                .description("Request identifier used for tracing"));

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                operation.addParametersItem(new Parameter()
                    .$ref("#/components/parameters/XRequestId"));
                Map.of(
                    "400", "Invalid request",
                    "401", "Invalid or missing API Key",
                    "403", "API Key lacks the required scope",
                    "404", "Requested region, event, or assessment was not found",
                    "409", "Resource or idempotency conflict",
                    "422", "Business data cannot produce a result",
                    "429", "Client rate limit exceeded",
                    "500", "Internal server error"
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

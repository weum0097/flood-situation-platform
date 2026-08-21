package com.example.flood.common.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}

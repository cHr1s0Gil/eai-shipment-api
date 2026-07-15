package com.eaishipment.config.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    private static final String API_KEY_SCHEME_NAME = "x-api-key";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().components(new Components().addSecuritySchemes(API_KEY_SCHEME_NAME,
                new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name(API_KEY_SCHEME_NAME)))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME_NAME));
    }
}

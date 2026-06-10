package com.carecircle.api.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the CareCircle backend.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CareCircle API",
                version = "0.1.0",
                description = "Backend API for CareCircle MVP."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}

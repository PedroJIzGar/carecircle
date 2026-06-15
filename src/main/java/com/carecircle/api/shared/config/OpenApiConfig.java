package com.carecircle.api.shared.config;

import com.carecircle.api.shared.exception.ApiErrorCode;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

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

    private static final String API_ERROR_RESPONSE_SCHEMA = "ApiErrorResponse";
    private static final String API_FIELD_ERROR_SCHEMA = "ApiFieldError";

    /**
     * Registers reusable schemas for the standard API error contract.
     *
     * @return OpenAPI customizer that adds error schemas under components.
     */
    @Bean
    OpenApiCustomizer apiErrorSchemasOpenApiCustomizer() {
        return openApi -> {
            Components components = getOrCreateComponents(openApi);
            components.addSchemas(API_FIELD_ERROR_SCHEMA, apiFieldErrorSchema());
            components.addSchemas(API_ERROR_RESPONSE_SCHEMA, apiErrorResponseSchema());
        };
    }

    /**
     * Adds common handled error responses to every documented API operation.
     *
     * <p>Springdoc does not infer all possible responses from {@code @RestControllerAdvice}
     * and Spring Security handlers. This central customizer keeps Swagger aligned
     * with the actual API error contract without repeating annotations on every
     * controller method.</p>
     *
     * @return operation customizer adding standard error responses.
     */
    @Bean
    OperationCustomizer apiErrorResponsesOperationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            addErrorResponseIfAbsent(responses, "400", "Validation error or malformed request.");
            addErrorResponseIfAbsent(responses, "401", "Missing, expired, or invalid bearer token.");
            addErrorResponseIfAbsent(responses, "403", "Authenticated user is not allowed to perform the operation.");
            addErrorResponseIfAbsent(responses, "404", "Requested resource does not exist or is not visible.");
            addErrorResponseIfAbsent(responses, "409", "Request conflicts with current application state.");
            addErrorResponseIfAbsent(responses, "500", "Unexpected server-side error.");

            return operation;
        };
    }

    private Components getOrCreateComponents(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            components = new Components();
            openApi.setComponents(components);
        }
        return components;
    }

    private Schema<?> apiFieldErrorSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.description("Field-level validation error returned by the API.");
        schema.addProperty("field", new StringSchema().description("Request field that failed validation."));
        schema.addProperty("message", new StringSchema().description("Validation message for the field."));
        schema.addRequiredItem("field");
        schema.addRequiredItem("message");
        return schema;
    }

    private Schema<?> apiErrorResponseSchema() {
        ObjectSchema schema = new ObjectSchema();
        schema.description("Standard JSON error response for API failures handled by CareCircle.");
        schema.addProperty("timestamp", new DateTimeSchema().description("Time when the error response was produced."));
        schema.addProperty("status", new IntegerSchema().description("HTTP status code."));
        schema.addProperty("error", new StringSchema().description("Short HTTP error name."));
        schema.addProperty("code", new StringSchema()
                .description("Stable machine-readable error code.")
                ._enum(Arrays.stream(ApiErrorCode.values()).map(Enum::name).toList()));
        schema.addProperty("message", new StringSchema().description("Human-readable technical message."));
        schema.addProperty("path", new StringSchema().description("Request path."));
        schema.addProperty("traceId", new StringSchema().description("Per-error identifier for support/debugging."));
        schema.addProperty("fieldErrors", new ArraySchema()
                .description("Optional field-level validation details.")
                .items(new Schema<>().$ref("#/components/schemas/" + API_FIELD_ERROR_SCHEMA)));
        schema.addRequiredItem("timestamp");
        schema.addRequiredItem("status");
        schema.addRequiredItem("error");
        schema.addRequiredItem("code");
        schema.addRequiredItem("message");
        schema.addRequiredItem("path");
        schema.addRequiredItem("traceId");
        schema.addRequiredItem("fieldErrors");
        return schema;
    }

    private void addErrorResponseIfAbsent(ApiResponses responses, String statusCode, String description) {
        if (!responses.containsKey(statusCode)) {
            responses.addApiResponse(statusCode, new ApiResponse()
                    .description(description)
                    .content(new io.swagger.v3.oas.models.media.Content()
                            .addMediaType(
                                    MediaType.APPLICATION_JSON_VALUE,
                                    new io.swagger.v3.oas.models.media.MediaType()
                                            .schema(new Schema<>().$ref(
                                                    "#/components/schemas/" + API_ERROR_RESPONSE_SCHEMA
                                            ))
                            )));
        }
    }
}

package com.carecircle.api.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTests {

    private final OpenApiConfig openApiConfig = new OpenApiConfig();

    @Test
    void apiErrorSchemasCustomizerRegistersStandardErrorSchemas() {
        OpenAPI openApi = new OpenAPI();

        openApiConfig.apiErrorSchemasOpenApiCustomizer().customise(openApi);

        assertThat(openApi.getComponents().getSchemas())
                .containsKeys("ApiErrorResponse", "ApiFieldError");
    }

    @Test
    void apiErrorResponsesCustomizerAddsStandardErrorResponses() {
        Operation operation = new Operation().responses(new ApiResponses());

        openApiConfig.apiErrorResponsesOperationCustomizer().customize(operation, null);

        assertThat(operation.getResponses())
                .containsKeys("400", "401", "403", "404", "409", "500");
        assertThat(operation.getResponses()
                .get("401")
                .getContent()
                .get("application/json")
                .getSchema()
                .get$ref()
        ).isEqualTo("#/components/schemas/ApiErrorResponse");
    }
}

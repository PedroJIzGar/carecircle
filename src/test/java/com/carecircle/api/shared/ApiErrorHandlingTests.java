package com.carecircle.api.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorsIncludeStableCodeTraceIdAndFieldErrors() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/tasks", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "error-validation-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("title: must not be blank"))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    @Test
    void malformedJsonReturnsStableErrorBody() throws Exception {
        mockMvc.perform(post("/circles/{circleId}/tasks", UUID.randomUUID())
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "error-malformed-" + UUID.randomUUID() + "@example.com")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed."))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void invalidPathVariableReturnsValidationErrorBody() throws Exception {
        mockMvc.perform(get("/circles/not-a-uuid/tasks")
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "error-path-" + UUID.randomUUID() + "@example.com")
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'circleId'."))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("circleId"));
    }

    @Test
    void unknownRouteReturnsStandardNotFoundBody() throws Exception {
        mockMvc.perform(get("/unknown-route")
                        .with(jwt().jwt(token -> token
                                .subject(UUID.randomUUID().toString())
                                .claim("email", "error-route-" + UUID.randomUUID() + "@example.com")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found."))
                .andExpect(jsonPath("$.traceId", notNullValue()));
    }

    @Test
    void missingBearerTokenReturnsStandardUnauthorizedBody() throws Exception {
        mockMvc.perform(get("/circles/{circleId}/tasks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required or token is invalid."))
                .andExpect(jsonPath("$.traceId", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }
}

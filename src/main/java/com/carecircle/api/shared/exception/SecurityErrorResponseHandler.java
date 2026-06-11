package com.carecircle.api.shared.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Writes standardized JSON errors for failures handled inside Spring Security.
 */
@Component
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Handles missing or invalid authentication before controller execution.
     *
     * @param request current HTTP request.
     * @param response current HTTP response.
     * @param authException authentication exception.
     * @throws IOException when the response body cannot be written.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writeError(
                response,
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTHENTICATION_REQUIRED,
                "Authentication is required or token is invalid.",
                request
        );
    }

    /**
     * Handles authorization failures produced by Spring Security.
     *
     * @param request current HTTP request.
     * @param response current HTTP response.
     * @param accessDeniedException access denied exception.
     * @throws IOException when the response body cannot be written.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        writeError(
                response,
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                "Access is denied.",
                request
        );
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request
    ) throws IOException {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                UUID.randomUUID().toString(),
                List.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

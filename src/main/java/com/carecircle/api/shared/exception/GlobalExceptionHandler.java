package com.carecircle.api.shared.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Converts expected application exceptions into stable JSON API responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles expected application conflicts.
     *
     * @param exception conflict exception.
     * @param request current HTTP request.
     * @return conflict response.
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceConflict(
            ResourceConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.CONFLICT, ApiErrorCode.RESOURCE_CONFLICT, exception.getMessage(), request);
    }

    /**
     * Handles operations forbidden for the authenticated user's current role.
     *
     * @param exception forbidden operation exception.
     * @param request current HTTP request.
     * @return forbidden response.
     */
    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleForbiddenOperation(
            ForbiddenOperationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, exception.getMessage(), request);
    }

    /**
     * Handles security access denials that happen after authentication.
     *
     * @param exception access denied exception.
     * @param request current HTTP request.
     * @return forbidden response.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, "Access is denied.", request);
    }

    /**
     * Handles authentication failures raised inside controller execution.
     *
     * @param exception authentication exception.
     * @param request current HTTP request.
     * @return unauthorized response.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTHENTICATION_REQUIRED,
                "Authentication is required or token is invalid.",
                request
        );
    }

    /**
     * Handles resources that do not exist or are not visible to the user.
     *
     * @param exception not-found exception.
     * @param request current HTTP request.
     * @return not found response.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, exception.getMessage(), request);
    }

    /**
     * Handles unknown API routes in the MVC layer.
     *
     * @param exception missing route exception.
     * @param request current HTTP request.
     * @return not found response.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND, "Resource not found.", request);
    }

    /**
     * Handles expected invalid request rules raised by application services.
     *
     * @param exception invalid request exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, exception.getMessage(), request);
    }

    /**
     * Handles invalid input raised by framework or defensive argument checks.
     *
     * @param exception validation exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, exception.getMessage(), request);
    }

    /**
     * Handles request DTO validation failures.
     *
     * @param exception validation exception produced by Jakarta Validation.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        String message = fieldErrors.stream()
                .findFirst()
                .map(error -> error.field() + ": " + error.message())
                .orElse("Request validation failed.");

        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message, request, fieldErrors);
    }

    /**
     * Handles direct method validation failures.
     *
     * @param exception validation exception produced by Spring method validation.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Request validation failed.",
                request
        );
    }

    /**
     * Handles constraint violations outside request body validation.
     *
     * @param exception constraint violation exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiFieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        String message = fieldErrors.stream()
                .findFirst()
                .map(error -> error.field() + ": " + error.message())
                .orElse("Request validation failed.");

        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message, request, fieldErrors);
    }

    /**
     * Handles invalid path variable or request parameter types.
     *
     * @param exception type mismatch exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '" + exception.getName() + "'.";
        List<ApiFieldError> fieldErrors = List.of(new ApiFieldError(exception.getName(), message));
        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message, request, fieldErrors);
    }

    /**
     * Handles missing required request parameters.
     *
     * @param exception missing parameter exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {
        String message = "Missing required parameter '" + exception.getParameterName() + "'.";
        List<ApiFieldError> fieldErrors = List.of(new ApiFieldError(exception.getParameterName(), message));
        return buildResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message, request, fieldErrors);
    }

    /**
     * Handles malformed JSON request bodies.
     *
     * @param exception unreadable HTTP message exception.
     * @param request current HTTP request.
     * @return bad request response.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.MALFORMED_REQUEST,
                "Request body is missing or malformed.",
                request
        );
    }

    /**
     * Handles database uniqueness and integrity conflicts.
     *
     * @param exception data integrity exception.
     * @param request current HTTP request.
     * @return conflict response.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.RESOURCE_CONFLICT,
                "Request conflicts with existing data.",
                request
        );
    }

    /**
     * Handles unexpected failures without leaking internal implementation details.
     *
     * @param exception unexpected exception.
     * @param request current HTTP request.
     * @return internal server error response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = generateTraceId();
        log.error("Unhandled API error. traceId={} path={}", traceId, request.getRequestURI(), exception);

        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ApiErrorCode.INTERNAL_ERROR,
                "Unexpected server error.",
                request.getRequestURI(),
                traceId,
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(status, code, message, request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request,
            List<ApiFieldError> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                generateTraceId(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}

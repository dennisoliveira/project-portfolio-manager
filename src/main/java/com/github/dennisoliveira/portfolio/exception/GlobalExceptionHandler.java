package com.github.dennisoliveira.portfolio.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TYPE_VALIDATION = "urn:problem:validation-error";
    private static final String TYPE_BUSINESS   = "urn:problem:business-rule";
    private static final String TYPE_NOT_FOUND  = "urn:problem:not-found";
    private static final String TYPE_FORBIDDEN  = "urn:problem:forbidden";
    private static final String TYPE_GENERIC    = "urn:problem:unexpected";

    // 404 — domínio
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create(TYPE_NOT_FOUND));
        pd.setTitle("Resource not found");
        pd.setDetail(nonNull(ex.getMessage(), "Resource not found"));
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // 422 — regra de negócio (domínio)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessRuleException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        var pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create(TYPE_BUSINESS));
        pd.setTitle("Business rule violation");
        pd.setDetail(nonNull(ex.getMessage(), "Business rule violation"));
        addCommon(pd, req);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    // 400 — Bean Validation no body (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), "Validation failed");

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorPayload)
                .toList();

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(TYPE_VALIDATION));
        pd.setTitle("Validation error");
        pd.setDetail("One or more fields are invalid.");
        addCommon(pd, req);
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    // 400 — validação em params/path
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), "Constraint violation");

        var errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.<String, Object>of(
                        "field", cv.getPropertyPath().toString(),
                        "message", nonBlank(cv.getMessage(), "Invalid value"),
                        "code", mapCode(cv.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                ))
                .toList();

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(TYPE_VALIDATION));
        pd.setTitle("Validation error");
        pd.setDetail("One or more fields are invalid.");
        addCommon(pd, req);
        pd.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    // 400 — JSON malformado/body inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(TYPE_VALIDATION));
        pd.setTitle("Malformed JSON");
        pd.setDetail("Request body is invalid or unreadable.");
        addCommon(pd, req);
        return ResponseEntity.badRequest().body(pd);
    }

    // 400 — tipo inválido no path/query
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): field='{}', value='{}'",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getName(), String.valueOf(ex.getValue()));

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(TYPE_VALIDATION));
        pd.setTitle("Type mismatch");
        pd.setDetail("Parameter type is invalid.");
        addCommon(pd, req);
        pd.setProperty("errors", List.of(Map.of(
                "field", ex.getName(),
                "message", "invalid type",
                "code", "TYPE_MISMATCH",
                "rejectedValue", String.valueOf(ex.getValue())
        )));
        return ResponseEntity.badRequest().body(pd);
    }

    // 405/415 — método ou content-type não suportados
    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
    public ResponseEntity<ProblemDetail> handleNotSupported(Exception ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        HttpStatus status = (ex instanceof HttpRequestMethodNotSupportedException)
                ? HttpStatus.METHOD_NOT_ALLOWED
                : HttpStatus.UNSUPPORTED_MEDIA_TYPE;

        var pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(TYPE_GENERIC));
        pd.setTitle("Not supported");
        pd.setDetail(nonNull(ex.getMessage(), "Request not supported"));
        addCommon(pd, req);
        return ResponseEntity.status(status).body(pd);
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create(TYPE_FORBIDDEN));
        pd.setTitle("Forbidden");
        pd.setDetail("Access is denied.");
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    // 4xx/5xx lançados como ErrorResponseException
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
        log.warn("{} on {} {} (traceId={}): {}",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex.getMessage());

        var pd = ex.getBody();
        if (pd.getTitle() == null) pd.setTitle(ex.getStatusCode().toString());
        if (pd.getDetail() == null) pd.setDetail(nonNull(ex.getMessage(), "Request error"));
        if (pd.getType() == null) pd.setType(URI.create(TYPE_GENERIC));
        addCommon(pd, req);
        return ResponseEntity.status(ex.getStatusCode()).body(pd);
    }

    // 500 — fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("{} on {} {} (traceId={})",
                ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), traceId(req), ex);

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create(TYPE_GENERIC));
        pd.setTitle("Unexpected error");
        pd.setDetail("An unexpected error occurred.");
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // --------- helpers ---------

    private Map<String, Object> toFieldErrorPayload(FieldError fe) {
        return Map.of(
                "field", fe.getField(),
                "message", nonBlank(fe.getDefaultMessage(), "Invalid value"),
                "code", mapCode(nonBlank(fe.getCode(), "Invalid"))
        );
    }

    private static void addCommon(ProblemDetail pd, HttpServletRequest req) {
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("method", nonNull(req.getMethod(), "UNKNOWN"));
        pd.setProperty("locale", LocaleContextHolder.getLocale().toLanguageTag());
        pd.setProperty("traceId", traceId(req));
    }

    private static String traceId(HttpServletRequest req) {
        String h = req.getHeader("X-Request-Id");
        return (h != null && !h.isBlank()) ? h : UUID.randomUUID().toString();
    }

    private static <T> T nonNull(T value, T fallback) {
        return value != null ? value : fallback;
    }

    private static String nonBlank(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private static String mapCode(String beanValidationCode) {
        return switch (beanValidationCode) {
            case "NotNull"        -> "REQUIRED";
            case "NotBlank"       -> "REQUIRED";
            case "Positive"       -> "POSITIVE_REQUIRED";
            case "PositiveOrZero" -> "NON_NEGATIVE_REQUIRED";
            case "Past"           -> "MUST_BE_PAST";
            case "Future"         -> "MUST_BE_FUTURE";
            case "Size"           -> "SIZE_VIOLATION";
            case "Pattern"        -> "PATTERN_VIOLATION";
            case "Email"          -> "EMAIL_INVALID";
            default               -> beanValidationCode != null
                    ? beanValidationCode.toUpperCase(Locale.ROOT)
                    : "INVALID";
        };
    }
}
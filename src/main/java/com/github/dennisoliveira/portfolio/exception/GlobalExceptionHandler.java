package com.github.dennisoliveira.portfolio.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.Instant.now;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 — recurso não encontrado (exceção de domínio)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Resource not found");
        pd.setDetail(nonNull(ex.getMessage(), "Resource not found"));
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // 422 — regra de negócio violada (exceção de domínio)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessRuleException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setTitle("Business rule violation");
        pd.setDetail(nonNull(ex.getMessage(), "Business rule violation"));
        addCommon(pd, req);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    // 400 — erros de validação (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> nonNull(fe.getDefaultMessage(), nonNull(fe.getCode(), "Invalid value")),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation error");
        pd.setDetail("One or more fields are invalid.");
        addCommon(pd, req);
        pd.setProperty("details", errors);
        return ResponseEntity.badRequest().body(pd);
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail(nonNull(ex.getMessage(), "Access is denied."));
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    // 4xx/5xx lançados como ErrorResponseException
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException ex, HttpServletRequest req) {
        var pd = ex.getBody();
        if (pd.getTitle() == null) {
            pd.setTitle(ex.getStatusCode().toString());
        }
        if (pd.getDetail() == null) {
            pd.setDetail(nonNull(ex.getMessage(), "Request error"));
        }
        addCommon(pd, req);
        return ResponseEntity.status(ex.getStatusCode()).body(pd);
    }

    // 500 — fallback genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Unexpected error");
        pd.setDetail("An unexpected error occurred.");
        addCommon(pd, req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // Propriedades comuns adicionadas a todas as respostas de erro
    private static void addCommon(ProblemDetail pd, HttpServletRequest req) {
        pd.setProperty("timestamp", now().toString());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("method", nonNull(req.getMethod(), "UNKNOWN"));
    }

    private static <T> T nonNull(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
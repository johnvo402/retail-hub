package com.johnvo.retailhub.api.exception;

import com.johnvo.retailhub.application.features.inventory.common.InventoryConcurrencyException;
import com.johnvo.retailhub.domain.shared.DomainException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiProblem> validation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), ignored -> new java.util.ArrayList<>())
                        .add(error.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                "One or more fields are invalid", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiProblem> constraintValidation(ConstraintViolationException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), List.of(violation.getMessage())));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
                "One or more parameters are invalid", errors);
    }

    @ExceptionHandler(MissingRequestValueException.class)
    ResponseEntity<ApiProblem> missingValue(MissingRequestValueException exception) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request",
                exception.getMessage(), Map.of());
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class,
            InventoryConcurrencyException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiProblem> conflict(Exception exception) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", "Conflict",
                "The resource changed concurrently or conflicts with existing data", Map.of());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiProblem> businessRule(DomainException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION",
                "Business rule violation", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiProblem> unexpected(Exception exception) {
        log.error("Unhandled request failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error",
                "An unexpected error occurred", Map.of());
    }

    private static ResponseEntity<ApiProblem> problem(HttpStatus status, String type, String title,
                                                      String detail, Map<String, List<String>> errors) {
        return ResponseEntity.status(status)
                .body(new ApiProblem(type, title, status.value(), detail, errors, traceId()));
    }

    public static String traceId() {
        String traceId = MDC.get("traceId");
        return traceId == null ? "unavailable" : traceId;
    }
}

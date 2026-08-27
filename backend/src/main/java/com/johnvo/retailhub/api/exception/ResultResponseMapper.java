package com.johnvo.retailhub.api.exception;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

@Component
public class ResultResponseMapper {
    public <T> ResponseEntity<?> ok(Result<T> result) {
        return map(result, ResponseEntity::ok);
    }

    public ResponseEntity<?> noContent(Result<?> result) {
        return map(result, ignored -> ResponseEntity.noContent().build());
    }

    public <T> ResponseEntity<?> created(Result<T> result, Function<T, URI> location) {
        return map(result, value -> ResponseEntity.created(location.apply(value)).body(value));
    }

    public <T, R> ResponseEntity<?> map(Result<T> result, Function<T, ResponseEntity<R>> success) {
        return result.isSuccess() ? success.apply(result.value()) : problem(result.error());
    }

    public ResponseEntity<ApiProblem> problem(ApplicationError error) {
        HttpStatus status = status(error.type());
        return ResponseEntity.status(status).body(new ApiProblem(
                error.code(), title(error.type()), status.value(), error.message(), Map.of(),
                GlobalExceptionHandler.traceId()));
    }

    private static HttpStatus status(ErrorType type) {
        return switch (type) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case BUSINESS_RULE -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
    }

    private static String title(ErrorType type) {
        return switch (type) {
            case VALIDATION -> "Validation failed";
            case NOT_FOUND -> "Resource not found";
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            case CONFLICT -> "Conflict";
            case BUSINESS_RULE -> "Business rule violation";
        };
    }
}

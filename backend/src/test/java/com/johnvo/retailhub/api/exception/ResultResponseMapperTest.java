package com.johnvo.retailhub.api.exception;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ResultResponseMapperTest {
    private final ResultResponseMapper mapper = new ResultResponseMapper();

    @Test
    void mapsApplicationErrorTypesToHttpStatuses() {
        assertStatus(ErrorType.VALIDATION, HttpStatus.BAD_REQUEST);
        assertStatus(ErrorType.NOT_FOUND, HttpStatus.NOT_FOUND);
        assertStatus(ErrorType.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        assertStatus(ErrorType.FORBIDDEN, HttpStatus.FORBIDDEN);
        assertStatus(ErrorType.CONFLICT, HttpStatus.CONFLICT);
        assertStatus(ErrorType.BUSINESS_RULE, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void unexpectedExceptionStillMapsToInternalServerError() {
        var response = new GlobalExceptionHandler().unexpected(new IllegalStateException("database unavailable"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo("INTERNAL_ERROR");
    }

    private void assertStatus(ErrorType type, HttpStatus expected) {
        var response = mapper.ok(Result.failure(new ApplicationError("TEST_ERROR", "Test failure", type)));
        assertThat(response.getStatusCode()).isEqualTo(expected);
    }
}

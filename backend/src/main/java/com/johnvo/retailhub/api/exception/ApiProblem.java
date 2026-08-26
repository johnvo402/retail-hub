package com.johnvo.retailhub.api.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        Map<String, List<String>> errors,
        String traceId
) {
}


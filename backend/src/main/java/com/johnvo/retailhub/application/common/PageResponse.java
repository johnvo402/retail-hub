package com.johnvo.retailhub.application.common;

import com.johnvo.retailhub.domain.shared.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
    public PageResponse {
        items = List.copyOf(items);
    }

    public static <S, T> PageResponse<T> from(PageResult<S> source, Function<S, T> mapper) {
        return new PageResponse<>(source.items().stream().map(mapper).toList(), source.page(),
                source.size(), source.totalItems(), source.totalPages());
    }
}


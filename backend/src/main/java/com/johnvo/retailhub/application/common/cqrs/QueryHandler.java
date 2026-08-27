package com.johnvo.retailhub.application.common.cqrs;

import com.johnvo.retailhub.application.common.Result;

public interface QueryHandler<Q extends Query<R>, R> {
    Result<R> handle(Q query);
}

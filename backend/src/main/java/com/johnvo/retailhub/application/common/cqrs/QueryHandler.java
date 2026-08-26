package com.johnvo.retailhub.application.common.cqrs;

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}


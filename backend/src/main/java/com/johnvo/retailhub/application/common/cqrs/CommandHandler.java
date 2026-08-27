package com.johnvo.retailhub.application.common.cqrs;

import com.johnvo.retailhub.application.common.Result;

public interface CommandHandler<C extends Command<R>, R> {
    Result<R> handle(C command);
}

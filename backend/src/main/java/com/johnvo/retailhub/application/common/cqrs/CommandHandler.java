package com.johnvo.retailhub.application.common.cqrs;

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}


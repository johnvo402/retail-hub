package com.johnvo.retailhub.application.features.auth.command.register;

import com.johnvo.retailhub.application.common.cqrs.Command;
import com.johnvo.retailhub.application.features.auth.common.UserView;

public record RegisterCommand(String email, String password) implements Command<UserView> {
}


package com.johnvo.retailhub.application.features.auth.command.login;

import com.johnvo.retailhub.application.common.cqrs.Command;
import com.johnvo.retailhub.application.features.auth.common.AuthenticationResult;

public record LoginCommand(String email, String password, String userAgent)
        implements Command<AuthenticationResult> {
}


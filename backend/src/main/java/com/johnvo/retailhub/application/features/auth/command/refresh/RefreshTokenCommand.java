package com.johnvo.retailhub.application.features.auth.command.refresh;

import com.johnvo.retailhub.application.common.cqrs.Command;
import com.johnvo.retailhub.application.features.auth.common.AuthenticationResult;

public record RefreshTokenCommand(String refreshToken, String userAgent)
        implements Command<AuthenticationResult> {
}


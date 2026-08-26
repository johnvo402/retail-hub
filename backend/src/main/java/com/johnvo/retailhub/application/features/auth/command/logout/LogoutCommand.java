package com.johnvo.retailhub.application.features.auth.command.logout;

import com.johnvo.retailhub.application.common.cqrs.Command;

public record LogoutCommand(String refreshToken) implements Command<Void> {
}


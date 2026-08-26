package com.johnvo.retailhub.application.features.auth.command.logoutall;

import com.johnvo.retailhub.application.common.cqrs.Command;

import java.util.UUID;

public record LogoutAllCommand(UUID userId) implements Command<Integer> {
}


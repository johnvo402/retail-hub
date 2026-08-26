package com.johnvo.retailhub.application.features.auth.command.logout;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class LogoutCommandHandler implements CommandHandler<LogoutCommand, Void> {
    private final AuthSessionRepository sessions;
    private final TokenService tokens;
    private final Clock clock;

    public LogoutCommandHandler(AuthSessionRepository sessions, TokenService tokens, Clock clock) {
        this.sessions = sessions;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Void handle(LogoutCommand command) {
        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            sessions.findActiveByTokenHashForUpdate(tokens.hashRefreshToken(command.refreshToken()))
                    .ifPresent(session -> {
                        session.revoke(clock.instant());
                        sessions.save(session);
                    });
        }
        return null;
    }
}


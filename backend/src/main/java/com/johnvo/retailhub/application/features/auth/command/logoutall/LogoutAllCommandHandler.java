package com.johnvo.retailhub.application.features.auth.command.logoutall;

import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class LogoutAllCommandHandler implements CommandHandler<LogoutAllCommand, Integer> {
    private final AuthSessionRepository sessions;
    private final Clock clock;

    public LogoutAllCommandHandler(AuthSessionRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Integer> handle(LogoutAllCommand command) {
        return Result.success(sessions.revokeAllActiveByUserId(command.userId(), clock.instant()));
    }
}

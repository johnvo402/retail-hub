package com.johnvo.retailhub.application.features.auth.command.refresh;

import com.johnvo.retailhub.application.common.UnauthorizedException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.application.features.auth.common.AuthenticationResult;
import com.johnvo.retailhub.application.features.auth.common.UserView;
import com.johnvo.retailhub.domain.identity.AuthSession;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RefreshTokenCommandHandler implements CommandHandler<RefreshTokenCommand, AuthenticationResult> {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final TokenService tokens;
    private final Clock clock;

    public RefreshTokenCommandHandler(UserRepository users, AuthSessionRepository sessions,
                                      TokenService tokens, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.tokens = tokens;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthenticationResult handle(RefreshTokenCommand command) {
        if (command.refreshToken() == null || command.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is missing");
        }
        String hash = tokens.hashRefreshToken(command.refreshToken());
        AuthSession oldSession = sessions.findActiveByTokenHashForUpdate(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        Instant now = clock.instant();
        try {
            oldSession.assertUsable(now);
        } catch (DomainException exception) {
            throw new UnauthorizedException(exception.getMessage());
        }
        User user = users.findById(oldSession.userId())
                .filter(User::active)
                .orElseThrow(() -> new UnauthorizedException("Account is unavailable"));

        oldSession.revoke(now);
        sessions.save(oldSession);
        TokenService.RefreshToken refresh = tokens.issueRefreshToken(now);
        sessions.save(AuthSession.create(user.id(), refresh.hash(), refresh.expiresAt(),
                sanitizeUserAgent(command.userAgent()), now));
        TokenService.AccessToken access = tokens.issueAccessToken(user);
        return new AuthenticationResult(access.value(), access.expiresInSeconds(), UserView.from(user),
                refresh.value(), refresh.expiresAt());
    }

    private static String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.substring(0, Math.min(userAgent.length(), 500));
    }
}


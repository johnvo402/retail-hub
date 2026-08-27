package com.johnvo.retailhub.application.features.auth.command.login;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.security.PasswordHasher;
import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.application.features.auth.common.AuthenticationResult;
import com.johnvo.retailhub.application.features.auth.common.UserView;
import com.johnvo.retailhub.domain.identity.AuthSession;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class LoginCommandHandler implements CommandHandler<LoginCommand, AuthenticationResult> {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final Clock clock;

    public LoginCommandHandler(UserRepository users, AuthSessionRepository sessions,
                               PasswordHasher passwordHasher, TokenService tokenService, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<AuthenticationResult> handle(LoginCommand command) {
        User user = users.findByEmail(command.email())
                .filter(User::active).orElse(null);
        if (user == null || !passwordHasher.matches(command.password(), user.passwordHash())) {
            return Result.failure(new ApplicationError(
                    "AUTH_INVALID_CREDENTIALS", "Email or password is incorrect", ErrorType.UNAUTHORIZED));
        }
        Instant now = clock.instant();
        TokenService.AccessToken access = tokenService.issueAccessToken(user);
        TokenService.RefreshToken refresh = tokenService.issueRefreshToken(now);
        sessions.save(AuthSession.create(user.id(), refresh.hash(), refresh.expiresAt(),
                sanitizeUserAgent(command.userAgent()), now));
        return Result.success(new AuthenticationResult(access.value(), access.expiresInSeconds(), UserView.from(user),
                refresh.value(), refresh.expiresAt()));
    }

    private static String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        return userAgent.substring(0, Math.min(userAgent.length(), 500));
    }
}

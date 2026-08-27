package com.johnvo.retailhub.application.features.auth;

import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.security.PasswordHasher;
import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.application.features.auth.command.login.LoginCommand;
import com.johnvo.retailhub.application.features.auth.command.login.LoginCommandHandler;
import com.johnvo.retailhub.domain.identity.AuthSession;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import com.johnvo.retailhub.domain.identity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {
    @Mock UserRepository users;
    @Mock AuthSessionRepository sessions;
    @Mock PasswordHasher passwords;
    @Mock TokenService tokens;

    private LoginCommandHandler handler;
    private User user;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-08-26T00:00:00Z");
        user = User.register("user@example.com", "encoded", UserRole.USER, now);
        handler = new LoginCommandHandler(users, sessions, passwords, tokens,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void successfulLoginReturnsAccessTokenAndStoresOnlyRefreshHash() {
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwords.matches("correct-password", "encoded")).thenReturn(true);
        when(tokens.issueAccessToken(user)).thenReturn(new TokenService.AccessToken("access", 900));
        when(tokens.issueRefreshToken(now)).thenReturn(new TokenService.RefreshToken(
                "raw-refresh", "hashed-refresh", now.plusSeconds(1200)));
        when(sessions.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = handler.handle(new LoginCommand(user.email(), "correct-password", "test-agent"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().accessToken()).isEqualTo("access");
        assertThat(result.value().refreshToken()).isEqualTo("raw-refresh");
        verify(sessions).save(org.mockito.ArgumentMatchers.argThat(session ->
                session.refreshTokenHash().equals("hashed-refresh")
                        && !session.refreshTokenHash().equals("raw-refresh")));
    }

    @Test
    void invalidPasswordIsRejected() {
        when(users.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwords.matches("wrong-password", "encoded")).thenReturn(false);

        var result = handler.handle(new LoginCommand(user.email(), "wrong-password", null));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(result.error().type()).isEqualTo(ErrorType.UNAUTHORIZED);
    }
}

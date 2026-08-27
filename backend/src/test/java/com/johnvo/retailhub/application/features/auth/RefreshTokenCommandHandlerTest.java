package com.johnvo.retailhub.application.features.auth;

import com.johnvo.retailhub.application.common.security.TokenService;
import com.johnvo.retailhub.application.features.auth.command.refresh.RefreshTokenCommand;
import com.johnvo.retailhub.application.features.auth.command.refresh.RefreshTokenCommandHandler;
import com.johnvo.retailhub.domain.identity.AuthSession;
import com.johnvo.retailhub.domain.identity.AuthSessionRepository;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import com.johnvo.retailhub.domain.identity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandHandlerTest {
    @Mock UserRepository users;
    @Mock AuthSessionRepository sessions;
    @Mock TokenService tokens;

    @Test
    void refreshRevokesOldSessionAndCreatesRotatedSession() {
        Instant now = Instant.parse("2026-08-26T00:00:00Z");
        User user = User.register("user@example.com", "hash", UserRole.USER, now);
        AuthSession old = AuthSession.create(user.id(), "old-hash", now.plusSeconds(600), null, now);
        when(tokens.hashRefreshToken("old-raw")).thenReturn("old-hash");
        when(sessions.findActiveByTokenHashForUpdate("old-hash")).thenReturn(Optional.of(old));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(tokens.issueRefreshToken(now)).thenReturn(new TokenService.RefreshToken(
                "new-raw", "new-hash", now.plusSeconds(1200)));
        when(tokens.issueAccessToken(user)).thenReturn(new TokenService.AccessToken("access", 900));
        when(sessions.save(any(AuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenCommandHandler handler = new RefreshTokenCommandHandler(users, sessions, tokens,
                Clock.fixed(now, ZoneOffset.UTC));

        var result = handler.handle(new RefreshTokenCommand("old-raw", "agent"));

        ArgumentCaptor<AuthSession> saved = ArgumentCaptor.forClass(AuthSession.class);
        org.mockito.Mockito.verify(sessions, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().getFirst().revokedAt()).isEqualTo(now);
        assertThat(saved.getAllValues().getLast().refreshTokenHash()).isEqualTo("new-hash");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().refreshToken()).isEqualTo("new-raw");
    }
}

package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.api.security.SecurityUtils;
import com.johnvo.retailhub.api.exception.ResultResponseMapper;
import com.johnvo.retailhub.application.features.auth.command.login.LoginCommand;
import com.johnvo.retailhub.application.features.auth.command.login.LoginCommandHandler;
import com.johnvo.retailhub.application.features.auth.command.logout.LogoutCommand;
import com.johnvo.retailhub.application.features.auth.command.logout.LogoutCommandHandler;
import com.johnvo.retailhub.application.features.auth.command.logoutall.LogoutAllCommand;
import com.johnvo.retailhub.application.features.auth.command.logoutall.LogoutAllCommandHandler;
import com.johnvo.retailhub.application.features.auth.command.refresh.RefreshTokenCommand;
import com.johnvo.retailhub.application.features.auth.command.refresh.RefreshTokenCommandHandler;
import com.johnvo.retailhub.application.features.auth.command.register.RegisterCommand;
import com.johnvo.retailhub.application.features.auth.command.register.RegisterCommandHandler;
import com.johnvo.retailhub.application.features.auth.common.AuthenticationResult;
import com.johnvo.retailhub.application.features.auth.common.UserView;
import com.johnvo.retailhub.application.features.auth.query.me.GetCurrentUserQuery;
import com.johnvo.retailhub.application.features.auth.query.me.GetCurrentUserQueryHandler;
import com.johnvo.retailhub.infrastructure.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public static final String REFRESH_COOKIE = "refresh_token";

    private final RegisterCommandHandler register;
    private final LoginCommandHandler login;
    private final RefreshTokenCommandHandler refresh;
    private final LogoutCommandHandler logout;
    private final LogoutAllCommandHandler logoutAll;
    private final GetCurrentUserQueryHandler currentUser;
    private final SecurityProperties properties;
    private final ResultResponseMapper results;

    public AuthController(RegisterCommandHandler register, LoginCommandHandler login,
                          RefreshTokenCommandHandler refresh, LogoutCommandHandler logout,
                          LogoutAllCommandHandler logoutAll, GetCurrentUserQueryHandler currentUser,
                          SecurityProperties properties, ResultResponseMapper results) {
        this.register = register;
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
        this.logoutAll = logoutAll;
        this.currentUser = currentUser;
        this.properties = properties;
        this.results = results;
    }

    @PostMapping("/register")
    ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        return results.map(register.handle(new RegisterCommand(request.email(), request.password())),
                user -> ResponseEntity.status(HttpStatus.CREATED).body(user));
    }

    @PostMapping("/login")
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return results.map(login.handle(new LoginCommand(request.email(), request.password(),
                servletRequest.getHeader("User-Agent"))), this::authenticatedResponse);
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String token,
            HttpServletRequest request) {
        return results.map(refresh.handle(new RefreshTokenCommand(token,
                request.getHeader("User-Agent"))), this::authenticatedResponse);
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
        return results.map(logout.handle(new LogoutCommand(token)), ignored -> ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build());
    }

    @PostMapping("/logout-all")
    ResponseEntity<?> logoutAll(Authentication authentication) {
        return results.map(logoutAll.handle(new LogoutAllCommand(SecurityUtils.userId(authentication))),
                ignored -> ResponseEntity.noContent()
                        .header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build());
    }

    @GetMapping("/me")
    ResponseEntity<?> me(Authentication authentication) {
        return results.ok(currentUser.handle(new GetCurrentUserQuery(SecurityUtils.userId(authentication))));
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(AuthenticationResult result) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result).toString())
                .body(new AuthResponse(result.accessToken(), result.expiresIn(), result.user()));
    }

    private ResponseCookie refreshCookie(AuthenticationResult result) {
        Duration maxAge = Duration.between(Instant.now(), result.refreshExpiresAt());
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path("/api/auth")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge);
        if (properties.cookieDomain() != null && !properties.cookieDomain().isBlank()) {
            builder.domain(properties.cookieDomain());
        }
        return builder.build();
    }

    private ResponseCookie clearCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path("/api/auth")
                .maxAge(Duration.ZERO);
        if (properties.cookieDomain() != null && !properties.cookieDomain().isBlank()) {
            builder.domain(properties.cookieDomain());
        }
        return builder.build();
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 72) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(max = 72) String password
    ) {
    }

    public record AuthResponse(String accessToken, long expiresIn, UserView user) {
    }
}

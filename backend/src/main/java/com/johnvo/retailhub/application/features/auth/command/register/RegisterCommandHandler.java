package com.johnvo.retailhub.application.features.auth.command.register;

import com.johnvo.retailhub.application.common.ApplicationError;
import com.johnvo.retailhub.application.common.ErrorType;
import com.johnvo.retailhub.application.common.Result;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.common.security.PasswordHasher;
import com.johnvo.retailhub.application.features.auth.common.UserView;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import com.johnvo.retailhub.domain.identity.UserRole;
import com.johnvo.retailhub.domain.shared.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RegisterCommandHandler implements CommandHandler<RegisterCommand, UserView> {
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterCommandHandler(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<UserView> handle(RegisterCommand command) {
        if (users.findByEmail(command.email()).isPresent()) {
            return Result.failure(new ApplicationError("AUTH_EMAIL_EXISTS",
                    "An account already exists for this email address", ErrorType.CONFLICT));
        }
        try {
            Instant now = clock.instant();
            User user = User.register(command.email(), passwordHasher.hash(command.password()), UserRole.USER, now);
            return Result.success(UserView.from(users.save(user)));
        } catch (DomainException exception) {
            return Result.failure(new ApplicationError(
                    "AUTH_REGISTRATION_INVALID", exception.getMessage(), ErrorType.VALIDATION));
        }
    }
}

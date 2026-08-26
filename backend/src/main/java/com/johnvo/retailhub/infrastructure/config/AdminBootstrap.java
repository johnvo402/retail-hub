package com.johnvo.retailhub.infrastructure.config;

import com.johnvo.retailhub.application.common.security.PasswordHasher;
import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import com.johnvo.retailhub.domain.identity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final BootstrapProperties properties;
    private final UserRepository users;
    private final PasswordHasher passwords;
    private final Clock clock;

    public AdminBootstrap(BootstrapProperties properties, UserRepository users,
                          PasswordHasher passwords, Clock clock) {
        this.properties = properties;
        this.users = users;
        this.passwords = passwords;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (properties.adminEmail() == null || properties.adminEmail().isBlank()
                || properties.adminPassword() == null || properties.adminPassword().isBlank()) {
            return;
        }
        users.findByEmail(properties.adminEmail()).orElseGet(() -> {
            User admin = User.register(properties.adminEmail(), passwords.hash(properties.adminPassword()),
                    UserRole.ADMIN, clock.instant());
            User saved = users.save(admin);
            log.info("Development administrator created for {}", saved.email());
            return saved;
        });
    }
}


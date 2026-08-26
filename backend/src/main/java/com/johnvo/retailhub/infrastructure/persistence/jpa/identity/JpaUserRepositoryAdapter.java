package com.johnvo.retailhub.infrastructure.persistence.jpa.identity;

import com.johnvo.retailhub.domain.identity.User;
import com.johnvo.retailhub.domain.identity.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository repository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(JpaUserRepositoryAdapter::toDomain);
    }

    @Override
    public User save(User user) {
        return toDomain(repository.save(new UserJpaEntity(user.id(), user.email(), user.passwordHash(),
                user.role(), user.active(), user.createdAt(), user.updatedAt())));
    }

    private static User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getRole(),
                entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}


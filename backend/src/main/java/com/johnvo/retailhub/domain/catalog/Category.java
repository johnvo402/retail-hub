package com.johnvo.retailhub.domain.catalog;

import com.johnvo.retailhub.domain.shared.DomainException;

import java.time.Instant;
import java.util.Objects;

public final class Category {
    private final CategoryId id;
    private String name;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Category(CategoryId id, String name, String description, boolean active,
                     Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.createdAt = Objects.requireNonNull(createdAt);
        update(name, description, active, updatedAt);
    }

    public static Category create(CategoryId id, String name, String description, Instant now) {
        return new Category(id, name, description, true, now, now);
    }

    public static Category reconstitute(CategoryId id, String name, String description,
                                        boolean active, Instant createdAt, Instant updatedAt) {
        return new Category(id, name, description, active, createdAt, updatedAt);
    }

    public void update(String name, String description, boolean active, Instant now) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new DomainException("Category name must contain between 1 and 120 characters");
        }
        this.name = name.trim();
        this.description = description == null ? "" : description.trim();
        this.active = active;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void deactivate(Instant now) {
        active = false;
        updatedAt = Objects.requireNonNull(now);
    }

    public CategoryId id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}


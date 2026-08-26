package com.johnvo.retailhub.domain.shared;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot {
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private long version;

    protected final void raise(DomainEvent event) {
        apply(event);
        uncommittedEvents.add(event);
        version++;
    }

    protected final void replay(DomainEvent event) {
        apply(event);
        version++;
    }

    protected abstract void apply(DomainEvent event);

    public final List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(uncommittedEvents);
    }

    public final void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    public final long getVersion() {
        return version;
    }
}


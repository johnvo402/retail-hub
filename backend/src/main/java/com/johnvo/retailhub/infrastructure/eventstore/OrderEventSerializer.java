package com.johnvo.retailhub.infrastructure.eventstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnvo.retailhub.domain.ordering.events.OrderCancelled;
import com.johnvo.retailhub.domain.ordering.events.OrderConfirmed;
import com.johnvo.retailhub.domain.ordering.events.OrderCreated;
import com.johnvo.retailhub.domain.ordering.events.OrderEvent;
import com.johnvo.retailhub.domain.ordering.events.OrderItemAdded;
import com.johnvo.retailhub.domain.ordering.events.OrderItemRemoved;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderEventSerializer {
    private static final Map<String, Class<? extends OrderEvent>> EVENT_TYPES = Map.of(
            OrderCreated.class.getSimpleName(), OrderCreated.class,
            OrderItemAdded.class.getSimpleName(), OrderItemAdded.class,
            OrderItemRemoved.class.getSimpleName(), OrderItemRemoved.class,
            OrderConfirmed.class.getSimpleName(), OrderConfirmed.class,
            OrderCancelled.class.getSimpleName(), OrderCancelled.class
    );

    private final ObjectMapper objectMapper;

    public OrderEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode serialize(OrderEvent event) {
        return objectMapper.valueToTree(event);
    }

    public OrderEvent deserialize(String eventType, JsonNode payload) {
        Class<? extends OrderEvent> type = EVENT_TYPES.get(eventType);
        if (type == null) {
            throw new IllegalStateException("Unsupported stored order event type: " + eventType);
        }
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize order event " + eventType, exception);
        }
    }
}


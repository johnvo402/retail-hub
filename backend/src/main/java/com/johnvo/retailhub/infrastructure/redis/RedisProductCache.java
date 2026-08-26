package com.johnvo.retailhub.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnvo.retailhub.application.features.catalog.common.ProductCache;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.infrastructure.config.CacheProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RedisProductCache implements ProductCache {
    private static final String PREFIX = "product:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;

    public RedisProductCache(StringRedisTemplate redis, ObjectMapper objectMapper,
                             CacheProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<ProductView> get(UUID productId) {
        String json = redis.opsForValue().get(key(productId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ProductView.class));
        } catch (JsonProcessingException exception) {
            redis.delete(key(productId));
            return Optional.empty();
        }
    }

    @Override
    public void put(ProductView product) {
        try {
            redis.opsForValue().set(key(product.id()), objectMapper.writeValueAsString(product),
                    properties.productTtl());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize cached product", exception);
        }
    }

    @Override
    public void evict(UUID productId) {
        redis.delete(key(productId));
    }

    private static String key(UUID productId) {
        return PREFIX + productId;
    }
}


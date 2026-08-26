package com.johnvo.retailhub.infrastructure.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(indexName = "products", createIndex = true)
public class ProductSearchDocument {
    @Id
    private UUID id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String sku;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private UUID categoryId;

    @Field(type = FieldType.Text)
    private String categoryName;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    protected ProductSearchDocument() {
    }

    public ProductSearchDocument(UUID id, String name, String description, String sku, BigDecimal price,
                                 UUID categoryId, String categoryName, boolean active,
                                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.price = price;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSku() { return sku; }
    public BigDecimal getPrice() { return price; }
    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}


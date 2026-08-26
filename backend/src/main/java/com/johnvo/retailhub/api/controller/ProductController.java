package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.catalog.command.createproduct.CreateProductCommand;
import com.johnvo.retailhub.application.features.catalog.command.createproduct.CreateProductCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.deleteproduct.DeleteProductCommand;
import com.johnvo.retailhub.application.features.catalog.command.deleteproduct.DeleteProductCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.reindexproducts.ReindexProductsCommand;
import com.johnvo.retailhub.application.features.catalog.command.reindexproducts.ReindexProductsCommandHandler;
import com.johnvo.retailhub.application.features.catalog.command.updateproduct.UpdateProductCommand;
import com.johnvo.retailhub.application.features.catalog.command.updateproduct.UpdateProductCommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductView;
import com.johnvo.retailhub.application.features.catalog.query.getproduct.GetProductQuery;
import com.johnvo.retailhub.application.features.catalog.query.getproduct.GetProductQueryHandler;
import com.johnvo.retailhub.application.features.catalog.query.getproducts.GetProductsQuery;
import com.johnvo.retailhub.application.features.catalog.query.getproducts.GetProductsQueryHandler;
import com.johnvo.retailhub.application.features.catalog.query.searchproducts.SearchProductsQuery;
import com.johnvo.retailhub.application.features.catalog.query.searchproducts.SearchProductsQueryHandler;
import com.johnvo.retailhub.domain.catalog.ProductFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final CreateProductCommandHandler createProduct;
    private final UpdateProductCommandHandler updateProduct;
    private final DeleteProductCommandHandler deleteProduct;
    private final GetProductQueryHandler getProduct;
    private final GetProductsQueryHandler getProducts;
    private final SearchProductsQueryHandler searchProducts;
    private final ReindexProductsCommandHandler reindexProducts;

    public ProductController(CreateProductCommandHandler createProduct,
                             UpdateProductCommandHandler updateProduct,
                             DeleteProductCommandHandler deleteProduct,
                             GetProductQueryHandler getProduct,
                             GetProductsQueryHandler getProducts,
                             SearchProductsQueryHandler searchProducts,
                             ReindexProductsCommandHandler reindexProducts) {
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
        this.getProduct = getProduct;
        this.getProducts = getProducts;
        this.searchProducts = searchProducts;
        this.reindexProducts = reindexProducts;
    }

    @GetMapping
    PageResponse<ProductView> list(
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return getProducts.handle(new GetProductsQuery(new ProductFilter(category, minPrice, maxPrice,
                active, keyword, page, size, sort)));
    }

    @GetMapping("/{id}")
    ProductView get(@PathVariable UUID id) {
        return getProduct.handle(new GetProductQuery(id));
    }

    @GetMapping("/search")
    PageResponse<ProductView> search(@RequestParam(name = "q", defaultValue = "") String query,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return searchProducts.handle(new SearchProductsQuery(query, page, size));
    }

    @PostMapping
    ResponseEntity<CreatedId> create(@Valid @RequestBody ProductRequest request) {
        CreatedId result = createProduct.handle(new CreateProductCommand(request.name(), request.description(),
                request.sku(), request.price(), request.categoryId()));
        return ResponseEntity.created(URI.create("/api/products/" + result.id())).body(result);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        updateProduct.handle(new UpdateProductCommand(id, request.name(), request.description(), request.sku(),
                request.price(), request.categoryId(), request.active()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProduct.handle(new DeleteProductCommand(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search/reindex")
    Map<String, Integer> reindex() {
        return Map.of("indexed", reindexProducts.handle(new ReindexProductsCommand()));
    }

    public record ProductRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 10000) String description,
            @NotBlank @Size(max = 80) String sku,
            @NotNull @DecimalMin(value = "0.00") BigDecimal price,
            @NotNull UUID categoryId,
            boolean active
    ) {
    }
}


package com.johnvo.retailhub.application.features.catalog.command.deleteproduct;

import com.johnvo.retailhub.application.common.ResourceNotFoundException;
import com.johnvo.retailhub.application.common.cqrs.CommandHandler;
import com.johnvo.retailhub.application.features.catalog.common.ProductCache;
import com.johnvo.retailhub.application.features.catalog.common.ProductSearchIndex;
import com.johnvo.retailhub.domain.catalog.Product;
import com.johnvo.retailhub.domain.catalog.ProductId;
import com.johnvo.retailhub.domain.catalog.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class DeleteProductCommandHandler implements CommandHandler<DeleteProductCommand, Void> {
    private final ProductRepository products;
    private final ProductCache cache;
    private final ProductSearchIndex searchIndex;
    private final Clock clock;

    public DeleteProductCommandHandler(ProductRepository products, ProductCache cache,
                                       ProductSearchIndex searchIndex, Clock clock) {
        this.products = products;
        this.cache = cache;
        this.searchIndex = searchIndex;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Void handle(DeleteProductCommand command) {
        Product product = products.findById(new ProductId(command.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Product was not found"));
        product.deactivate(clock.instant());
        products.save(product);
        cache.evict(command.id());
        searchIndex.delete(command.id());
        return null;
    }
}


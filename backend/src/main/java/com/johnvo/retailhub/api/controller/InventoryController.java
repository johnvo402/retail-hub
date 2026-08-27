package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.api.exception.ResultResponseMapper;
import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.query.getinventory.GetInventoryQuery;
import com.johnvo.retailhub.application.features.inventory.query.getinventory.GetInventoryQueryHandler;
import com.johnvo.retailhub.application.features.inventory.query.getinventoryitems.GetInventoryItemsQuery;
import com.johnvo.retailhub.application.features.inventory.query.getinventoryitems.GetInventoryItemsQueryHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final GetInventoryItemsQueryHandler list;
    private final GetInventoryQueryHandler get;
    private final IncreaseStockCommandHandler increase;
    private final DecreaseStockCommandHandler decrease;
    private final ResultResponseMapper results;

    public InventoryController(GetInventoryItemsQueryHandler list, GetInventoryQueryHandler get,
                               IncreaseStockCommandHandler increase, DecreaseStockCommandHandler decrease,
                               ResultResponseMapper results) {
        this.list = list;
        this.get = get;
        this.increase = increase;
        this.decrease = decrease;
        this.results = results;
    }

    @GetMapping
    ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        return results.ok(list.handle(new GetInventoryItemsQuery(page, size, sort)));
    }

    @GetMapping("/{productId}")
    ResponseEntity<?> get(@PathVariable UUID productId) {
        return results.ok(get.handle(new GetInventoryQuery(productId)));
    }

    @PostMapping("/{productId}/increase")
    ResponseEntity<?> increase(@PathVariable UUID productId, @Valid @RequestBody AdjustmentRequest request) {
        return results.ok(increase.handle(new IncreaseStockCommand(productId, request.quantity())));
    }

    @PostMapping("/{productId}/decrease")
    ResponseEntity<?> decrease(@PathVariable UUID productId, @Valid @RequestBody AdjustmentRequest request) {
        return results.ok(decrease.handle(new DecreaseStockCommand(productId, request.quantity())));
    }

    public record AdjustmentRequest(@Min(1) int quantity) {
    }
}

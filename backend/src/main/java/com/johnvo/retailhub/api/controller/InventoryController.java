package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.decreasestock.DecreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommand;
import com.johnvo.retailhub.application.features.inventory.command.increasestock.IncreaseStockCommandHandler;
import com.johnvo.retailhub.application.features.inventory.common.InventoryView;
import com.johnvo.retailhub.application.features.inventory.common.StockAdjustmentResult;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final GetInventoryItemsQueryHandler list;
    private final GetInventoryQueryHandler get;
    private final IncreaseStockCommandHandler increase;
    private final DecreaseStockCommandHandler decrease;

    public InventoryController(GetInventoryItemsQueryHandler list, GetInventoryQueryHandler get,
                               IncreaseStockCommandHandler increase, DecreaseStockCommandHandler decrease) {
        this.list = list;
        this.get = get;
        this.increase = increase;
        this.decrease = decrease;
    }

    @GetMapping
    PageResponse<InventoryView> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        return list.handle(new GetInventoryItemsQuery(page, size, sort));
    }

    @GetMapping("/{productId}")
    InventoryView get(@PathVariable UUID productId) {
        return get.handle(new GetInventoryQuery(productId));
    }

    @PostMapping("/{productId}/increase")
    StockAdjustmentResult increase(@PathVariable UUID productId, @Valid @RequestBody AdjustmentRequest request) {
        return increase.handle(new IncreaseStockCommand(productId, request.quantity()));
    }

    @PostMapping("/{productId}/decrease")
    StockAdjustmentResult decrease(@PathVariable UUID productId, @Valid @RequestBody AdjustmentRequest request) {
        return decrease.handle(new DecreaseStockCommand(productId, request.quantity()));
    }

    public record AdjustmentRequest(@Min(1) int quantity) {
    }
}


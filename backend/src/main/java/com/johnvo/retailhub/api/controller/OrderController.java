package com.johnvo.retailhub.api.controller;

import com.johnvo.retailhub.api.security.SecurityUtils;
import com.johnvo.retailhub.application.common.CreatedId;
import com.johnvo.retailhub.application.common.PageResponse;
import com.johnvo.retailhub.application.features.ordering.command.addorderitem.AddOrderItemCommand;
import com.johnvo.retailhub.application.features.ordering.command.addorderitem.AddOrderItemCommandHandler;
import com.johnvo.retailhub.application.features.ordering.command.cancelorder.CancelOrderCommand;
import com.johnvo.retailhub.application.features.ordering.command.cancelorder.CancelOrderCommandHandler;
import com.johnvo.retailhub.application.features.ordering.command.confirmorder.ConfirmOrderCommand;
import com.johnvo.retailhub.application.features.ordering.command.confirmorder.ConfirmOrderCommandHandler;
import com.johnvo.retailhub.application.features.ordering.command.createorder.CreateOrderCommand;
import com.johnvo.retailhub.application.features.ordering.command.createorder.CreateOrderCommandHandler;
import com.johnvo.retailhub.application.features.ordering.command.removeorderitem.RemoveOrderItemCommand;
import com.johnvo.retailhub.application.features.ordering.command.removeorderitem.RemoveOrderItemCommandHandler;
import com.johnvo.retailhub.application.features.ordering.common.OrderView;
import com.johnvo.retailhub.application.features.ordering.query.getorder.GetOrderQuery;
import com.johnvo.retailhub.application.features.ordering.query.getorder.GetOrderQueryHandler;
import com.johnvo.retailhub.application.features.ordering.query.getorders.GetOrdersQuery;
import com.johnvo.retailhub.application.features.ordering.query.getorders.GetOrdersQueryHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderCommandHandler create;
    private final AddOrderItemCommandHandler addItem;
    private final RemoveOrderItemCommandHandler removeItem;
    private final ConfirmOrderCommandHandler confirm;
    private final CancelOrderCommandHandler cancel;
    private final GetOrderQueryHandler get;
    private final GetOrdersQueryHandler list;

    public OrderController(CreateOrderCommandHandler create, AddOrderItemCommandHandler addItem,
                           RemoveOrderItemCommandHandler removeItem, ConfirmOrderCommandHandler confirm,
                           CancelOrderCommandHandler cancel, GetOrderQueryHandler get,
                           GetOrdersQueryHandler list) {
        this.create = create;
        this.addItem = addItem;
        this.removeItem = removeItem;
        this.confirm = confirm;
        this.cancel = cancel;
        this.get = get;
        this.list = list;
    }

    @PostMapping
    ResponseEntity<CreatedId> create(Authentication authentication) {
        CreatedId result = create.handle(new CreateOrderCommand(SecurityUtils.userId(authentication)));
        return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
    }

    @PostMapping("/{id}/items")
    ResponseEntity<CreatedId> addItem(@PathVariable UUID id, @Valid @RequestBody AddItemRequest request,
                                     Authentication authentication) {
        CreatedId result = addItem.handle(new AddOrderItemCommand(id, request.productId(), request.quantity(),
                SecurityUtils.userId(authentication), SecurityUtils.isAdmin(authentication)));
        return ResponseEntity.created(URI.create("/api/orders/" + id + "/items/" + result.id())).body(result);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    ResponseEntity<Void> removeItem(@PathVariable UUID id, @PathVariable UUID itemId,
                                    Authentication authentication) {
        removeItem.handle(new RemoveOrderItemCommand(id, itemId, SecurityUtils.userId(authentication),
                SecurityUtils.isAdmin(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    ResponseEntity<Void> confirm(@PathVariable UUID id, Authentication authentication) {
        confirm.handle(new ConfirmOrderCommand(id, SecurityUtils.userId(authentication),
                SecurityUtils.isAdmin(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    ResponseEntity<Void> cancel(@PathVariable UUID id, Authentication authentication) {
        cancel.handle(new CancelOrderCommand(id, SecurityUtils.userId(authentication),
                SecurityUtils.isAdmin(authentication)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    PageResponse<OrderView> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 Authentication authentication) {
        return list.handle(new GetOrdersQuery(SecurityUtils.userId(authentication),
                SecurityUtils.isAdmin(authentication), page, size));
    }

    @GetMapping("/{id}")
    OrderView get(@PathVariable UUID id, Authentication authentication) {
        return get.handle(new GetOrderQuery(id, SecurityUtils.userId(authentication),
                SecurityUtils.isAdmin(authentication)));
    }

    public record AddItemRequest(@NotNull UUID productId, @Min(1) int quantity) {
    }
}

